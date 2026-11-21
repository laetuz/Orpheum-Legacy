package id.neotica.orpheum.feature.auth.forgotpassword.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.auth.forgotpassword.contract.ForgotPasswordView;
import id.neotica.orpheum.feature.auth.forgotpassword.presenter.ForgotPasswordPresenter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class ForgotPasswordActivity extends Activity implements ForgotPasswordView {

    private EditText etEmail;

    private ForgotPasswordPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_forgot_password);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.auth_forgot_title), true);

        presenter = new ForgotPasswordPresenter(this);
        presenter.attach(this);

        etEmail = (EditText) findViewById(R.id.et_forgot_email);
        Button btnSend = (Button) findViewById(R.id.btn_send_reset_link);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.submit(etEmail.getText().toString().trim());
            }
        });
    }

    @Override
    public void showEmailError() {
        etEmail.setError(getString(R.string.common_error_enter_email));
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
