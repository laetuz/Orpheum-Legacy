package id.neotica.orpheum.feature.subsonic.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.auth.common.ui.PasswordEyeToggle;
import id.neotica.orpheum.feature.home.ui.MainActivity;
import id.neotica.orpheum.feature.subsonic.contract.SubsonicLoginView;
import id.neotica.orpheum.feature.subsonic.presenter.SubsonicLoginPresenter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class SubsonicLoginActivity extends Activity implements SubsonicLoginView {

    private EditText etServer;
    private EditText etUsername;
    private EditText etPassword;

    private SubsonicLoginPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_subsonic_login);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.subsonic_title), true);

        presenter = new SubsonicLoginPresenter(this);
        presenter.attach(this);

        etServer = (EditText) findViewById(R.id.et_subsonic_server);
        etUsername = (EditText) findViewById(R.id.et_subsonic_username);
        etPassword = (EditText) findViewById(R.id.et_subsonic_password);
        Button btnConnect = (Button) findViewById(R.id.btn_subsonic_connect);

        PasswordEyeToggle.attach(etPassword, R.drawable.ic_eye_open, R.drawable.ic_eye_closed);

        findViewById(R.id.layout_root).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return false;
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.connect(etServer.getText().toString(),
                        etUsername.getText().toString(),
                        etPassword.getText().toString());
            }
        });
    }

    @Override
    public void showServerError() {
        etServer.setError(getString(R.string.subsonic_error_server));
    }

    @Override
    public void showInvalidServerError() {
        etServer.setError(getString(R.string.subsonic_error_invalid_server));
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
        Intent intent = new Intent(SubsonicLoginActivity.this, MainActivity.class);
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
