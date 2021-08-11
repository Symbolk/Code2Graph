package com.idunnolol.findviewbyid;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends ActionBarActivity {

    private Random mRandom;

    private Button mDepthButton;
    private Button mChildrenButton;
    private Button mRunButton;
    private Button mResetButton;

    private TextView mStatusTextView;
    private TextView mResultsTextView;
    private ViewGroup mContainer;

    private int mDepth = 0;
    private int mNumChildrenPerNode = 1;

    private int mId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRandom = new Random();

        setContentView(R.layout.activity_main);

        mDepthButton = (Button) findViewById(R.id.add_depth_button);
        mChildrenButton = (Button) findViewById(R.id.add_child_button);
        mRunButton = (Button) findViewById(R.id.run_test_button);
        mResetButton = (Button) findViewById(R.id.reset_button);
        mStatusTextView = (TextView) findViewById(R.id.status_text_view);
        mResultsTextView = (TextView) findViewById(R.id.results_text_view);
        mContainer = (ViewGroup) findViewById(R.id.test_container);

        mDepthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDepth++;
                updateStatus();
                checkTree(mContainer, 0);
                mResultsTextView.setText(null);
            }
        });
        mChildrenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumChildrenPerNode++;
                updateStatus();
                checkTree(mContainer, 0);
                mResultsTextView.setText(null);
            }
        });
        mRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mResultsTextView.setText("Running test...");
                runTest(getRunTimes());
            }
        });
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDepth = 0;
                mNumChildrenPerNode = 1;
                mId = 1;
                updateStatus();
                mResultsTextView.setText(null);
                reset(mContainer);
            }
        });

        updateStatus();
    }

    private void updateStatus() {
        mStatusTextView.setText("depth=" + mDepth + " viewsPerNode=" + mNumChildrenPerNode + " totalViews=" + getViewCount() + " testIterations=" + getRunTimes());
    }

    // Modulate based on number of nodes, so we don't ever quite overload the system
    // Minimum bound of 100
    private int getRunTimes() {
        return Math.max(100, 2000 - getViewCount());
    }

    private int getViewCount() {
        // TODO: I'm sure there's a more mathy way of doing this
        int count = 0;
        for (int a = 0; a < mDepth; a++) {
            count += (int) Math.pow(mNumChildrenPerNode, a + 1);
        }
        return count;
    }

    // Recursively checks each node to see if it follows the current params set out
    private void checkTree(ViewGroup view, int depth) {
        if (depth < mDepth) {
            // If we're not at max depth, check that this node has enough children
            while (view.getChildCount() < mNumChildrenPerNode) {
                view.addView(inflateChildLayout(view));
            }

            // For each child, check their integrity
            for (int a = 0; a < view.getChildCount(); a++) {
                checkTree((ViewGroup) view.getChildAt(a), depth + 1);
            }
        }

        // If this view group has children, depend on child to define width/height
        if (view.getChildCount() != 0) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    private void reset(ViewGroup view) {
        view.removeAllViews();
    }

    private ViewGroup inflateChildLayout(ViewGroup parent) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.child, parent, false);

        // Give it a random bg color so we can distinguish it in the UI
        int color = Color.rgb(mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
        viewGroup.setBackgroundColor(color);

        // Give it an ID we can use to look it up
        viewGroup.setId(mId);
        mId++;

        return viewGroup;
    }

    private void runTest(final int numTimes) {
        setButtonsEnabled(false);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                long start, end;
                long total = 0;
                for (int a = 0; a < numTimes; a++) {
                    start = System.nanoTime();

                    // Find past the FURTHEST id, so it has to scan all the nodes and then fails
                    mContainer.findViewById(mId);

                    end = System.nanoTime();
                    total += end - start;
                }

                long avg = total / numTimes;
                final String statusText = "Avg Time for findViewById(" + mId + "): " + avg + " ns (" + (avg / 1000000) + " ms)";

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mResultsTextView.setText(statusText);
                        setButtonsEnabled(true);
                    }
                });
            }
        });
        t.start();
    }

    private void setButtonsEnabled(boolean enabled) {
        mDepthButton.setEnabled(enabled);
        mChildrenButton.setEnabled(enabled);
        mRunButton.setEnabled(enabled);
        mResetButton.setEnabled(enabled);
    }

}
