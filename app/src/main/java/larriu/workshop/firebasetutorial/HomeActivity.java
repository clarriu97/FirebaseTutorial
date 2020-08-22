package larriu.workshop.firebasetutorial;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

enum  ProviderType {
    BASIC,
    GOOGLE,
    FACEBOOK
}

public class HomeActivity extends AppCompatActivity {

    private String email, provider;
    private Button logoutButton, errorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        logoutButton = findViewById(R.id.logoutButton);
        errorButton = findViewById(R.id.errorButton);

        Bundle bundle = getIntent().getExtras().getBundle("bundle");
        email = bundle.getString("email");
        provider = bundle.getString("provider");
        setUp(email, provider);

        //Guardado de datos
        SharedPreferences preferences = getSharedPreferences("credenciales", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("email", email);
        editor.putString("provider", provider);
        editor.commit();
    }

    private void setUp(String email, String provider) {
        setTitle("INICIO");
        ((TextView)findViewById(R.id.emailEditTextHomeActivity)).setText(email);
        ((TextView)findViewById(R.id.proveedorTextView)).setText(provider);

        logoutButton.setOnClickListener(new LogOutButtonListener());
        errorButton.setOnClickListener(new ErrorButtonListener());
    }

    private class LogOutButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            //Borrado de datos

            getSharedPreferences("credenciales", Context.MODE_PRIVATE).edit().clear().commit();

            if (provider.equals(ProviderType.FACEBOOK)){
                LoginManager.getInstance().logOut();
            }

            FirebaseAuth.getInstance().signOut();
            //onBackPressed();

            showAuthActivity();
        }
    }

    private void showAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    private class ErrorButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //Envio de información adicional
            FirebaseCrashlytics.getInstance().setUserId(email);
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider);

            //Enviar log de contexto
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el botón FORZAR ERROR");

            //Forzar error
            //throw new RuntimeException("Forzado de error");
        }
    }
}
