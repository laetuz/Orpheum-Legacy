package id.neotica.orpheum.feature.auth.login.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.auth.common.ui.PasswordEyeToggle;
import id.neotica.orpheum.feature.auth.forgotpassword.ui.ForgotPasswordActivity;
import id.neotica.orpheum.feature.auth.login.contract.LoginView;
import id.neotica.orpheum.feature.auth.login.presenter.LoginPresenter;
import id.neotica.orpheum.feature.auth.register.ui.RegisterActivity;
import id.neotica.orpheum.feature.home.ui.MainActivity;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class LoginActivity extends Activity implements LoginView {

    private EditText etUsername;
    private EditText etPassword;

    private LoginPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_login);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.auth_login_title), false);

        presenter = new LoginPresenter(this);
        presenter.attach(this);

        etUsername = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);
        Button btnLogin = (Button) findViewById(R.id.btn_login);
        Button btnRegister = (Button) findViewById(R.id.btn_register);
        TextView tvForgotPassword = (TextView) findViewById(R.id.tv_forgot_password);

        PasswordEyeToggle.attach(etPassword, R.drawable.ic_eye_open, R.drawable.ic_eye_closed);

        findViewById(R.id.layout_root).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return false;
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.login(etUsername.getText().toString().trim(),
                        etPassword.getText().toString().trim());
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    @Override
    public void showUsernameError() {
        etUsername.setError(getString(R.string.common_error_enter_username));
    }

    @Override
    public void showPasswordError() {
        etPassword.setError(getString(R.string.common_error_enter_password));
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
