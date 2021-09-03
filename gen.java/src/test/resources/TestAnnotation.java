package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

//  NormalAnnotation
@Register(id = "java-jdt", accept = "\\.java$", priority = Registry.Priority.MAXIMUM)
public class TestAnnotation extends AppCompatActivity {
    // SingleMemberAnnotation
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.button2)
    Button button2;

    // MarkerAnnotation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    // SingleMemberAnnotation
    @OnClick({R.id.button, R.id.button2})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.button:
                Toast.makeText(MainActivity.this,"点击button",Toast.LENGTH_SHORT).show();
                break;
            case R.id.button2:
                Intent intent = new Intent(MainActivity.this, com.example.mylibrary.MainActivity.class);
                startActivity(intent);
                break;
        }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@IndexAnnotated
@Target(ElementType.TYPE)
public @interface Register {
    String id();
    String[] accept() default { };
    int priority() default Registry.Priority.MEDIUM;
}
