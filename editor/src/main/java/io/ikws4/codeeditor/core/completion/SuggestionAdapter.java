package io.ikws4.codeeditor.core.completion;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.core.colorscheme.ColorScheme;
import io.ikws4.codeeditor.language.Suggestion;

public class SuggestionAdapter extends ArrayAdapter<Suggestion> {
    private ColorScheme mColorScheme;
    private float mTextSize;

    public SuggestionAdapter(@NonNull Context context) {
        super(context, R.layout.item_suggestion);
         mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14.0f, getContext().getResources().getDisplayMetrics());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Suggestion suggestion = getItem(position);

        ViewHolder viewHolder;
        if (convertView == null) { viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_suggestion, parent, false);
            viewHolder.type = convertView.findViewById(R.id.type);
            viewHolder.suggestion = convertView.findViewById(R.id.suggestion);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.type.setText(suggestion.getTypeString());
        viewHolder.suggestion.setText(suggestion.getText());

        viewHolder.type.setTextSize(mTextSize);
        viewHolder.suggestion.setTextSize(mTextSize);

        if (mColorScheme != null) {
            viewHolder.type.setTextColor(mColorScheme.getTextColor());
            viewHolder.suggestion.setTextColor(mColorScheme.getTextColor());
        }

        return convertView;
    }

    public void setData(List<Suggestion> suggestions) {
        clear();
        addAll(suggestions);
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        mColorScheme = colorScheme;
    }

    private static class ViewHolder {
        TextView type;
        TextView suggestion;
    }
}
