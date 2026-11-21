package id.neotica.orpheum.feature.auth.register.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.auth.register.contract.RegisterView;
import id.neotica.orpheum.feature.auth.register.presenter.RegisterPresenter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class RegisterActivity extends Activity implements RegisterView {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;

    private RegisterPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_register);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.auth_register_title), true);

        presenter = new RegisterPresenter(this);
        presenter.attach(this);

        etUsername = (EditText) findViewById(R.id.et_register_username);
        etEmail = (EditText) findViewById(R.id.et_register_email);
        etPassword = (EditText) findViewById(R.id.et_register_password);
        Button btnRegister = (Button) findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.register(etUsername.getText().toString().trim(),
                        etEmail.getText().toString().trim(),
                        etPassword.getText().toString().trim());
            }
        });
    }

    @Override
    public void showUsernameError() {
        etUsername.setError(getString(R.string.common_error_enter_username));
    }

    @Override
    public void showEmailError() {
        etEmail.setError(getString(R.string.common_error_enter_email));
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
    public void finishScreen() {
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
