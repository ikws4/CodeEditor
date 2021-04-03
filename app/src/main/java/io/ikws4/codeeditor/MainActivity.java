package io.ikws4.codeeditor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.ikws4.codeeditor.core.CodeEditor;
import io.ikws4.codeeditor.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CodeEditor editor = binding.editor;
        editor.setText(getExampleJavaSourceCode());

        binding.toolbar.setOnMenuItemClickListener((menu) -> {
            int id = menu.getItemId();
            if (id == R.id.undo) {
                editor.undo();
            } else if (id == R.id.redo) {
                editor.redo();
            } else if (id == R.id.format) {
                editor.format();
            } else {
                return false;
            }
            return true;
        });

        binding.keyboardBar.attach(editor);
    }

    private String getExampleJavaSourceCode() {
        try {
            InputStream inputStream = getAssets().open("example.java");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }
}