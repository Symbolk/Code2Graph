package com.mayank.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.mayank.R;
import com.mayank.databinding.LoginBindings;
import com.mayank.models.LoginViewModel;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private LoginBindings mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        mBinding = DataBindingUtil.setContentView(this, R.layout.login_page);

        // Initializing the bindings @XML
        initializeBindings();
    }

    private void initializeBindings() {
        mBinding.setHandler(this);
        mBinding.setEnableLoginButton(true);
        mBinding.setEnableResetButton(false);

        // Setting the values of bindings via model class
        mBinding.setLoginViewModel(new LoginViewModel(this, mBinding));

        mBinding.userNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (start >= 4 && mBinding.userPasswordField.length() >= 4) {
                    mBinding.setEnableResetButton(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mBinding.userPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (start >= 4 && mBinding.userNameField.length() >= 4) {
                    mBinding.setEnableResetButton(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonLogin:
                if (mBinding.getLoginViewModel().validateUserName() && mBinding.getLoginViewModel().validatePassword()) {
                    // Proceed To login
                    startActivity(new Intent(this, LoginSuccessActivity.class));
                    disableResetButton();
                }
                break;
            case R.id.buttonReset:
                // Reset fields & disable
                disableResetButton();
                break;
        }
    }

    private void disableResetButton() {
        mBinding.setEnableResetButton(false);
        mBinding.userPasswordField.setText(null);
        mBinding.userNameField.setText(null);
    }
}