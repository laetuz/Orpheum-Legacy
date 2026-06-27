package id.neotica.orpheum.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import id.neotica.orpheum.R;
import id.neotica.orpheum.utils.AuthManager;
import id.neotica.orpheum.utils.CrashCatcher;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(this.getApplicationContext());
        setContentView(R.layout.activity_main);
        CrashCatcher.showCrashLogIfAny(this);

        final TextView tvTitle = (TextView) findViewById(R.id.tv_title);
        final Button btLogin = (Button) findViewById(R.id.btn_login);
        final Button btRegister = (Button) findViewById(R.id.btn_register);
        final AuthManager authManager = new AuthManager(this);

        if (authManager.isLoggedIn()) {
            btLogin.setVisibility(View.GONE);
            btRegister.setVisibility(View.GONE);
            tvTitle.setVisibility(View.VISIBLE);
            String username = authManager.getUsernameFromToken();
            tvTitle.setText(username != null ? "Welcome " + username + "!" : "Welcome User!");
            tvTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
        } else {
            btLogin.setVisibility(View.VISIBLE);
            btRegister.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.GONE);
            btLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            });
            btRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                }
            });
        }
    }
}
