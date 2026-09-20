/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;

import net.gsantner.markor.R;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers a color per file/folder (keyed by absolute path) and paints file-browser rows
 * ColorNote-style: pale tinted row + solid strip on the left edge.
 * <p>
 * Three responsibilities, kept in one class so integration stays small:
 * 1. storage:  get / set / move
 * 2. styling:  applyToRow
 * 3. picking:  showPicker
 */
public class FileColors {
    public static final int NONE = 0;

    private static final String PREFS_NAME = "file_colors";

    private static final int[] COLORS = {
            NONE,
            0xFFF2E600, // yellow
            0xFFF5A000, // orange
            0xFF2EB82E, // green
            0xFF1F6FD1, // blue
            0xFF8E44AD, // purple
            0xFFD9363E, // red
            0xFF9E9E9E, // gray
    };

    private static final int STRIP_WIDTH_DP = 6;


    private final SharedPreferences _prefs;

    public FileColors(final Context context) {
        _prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- storage

    public int get(final File file) {
        return file == null ? NONE : _prefs.getInt(file.getAbsolutePath(), NONE);
    }

    public void set(final File file, final int color) {
        if (file == null) {
            return;
        }
        final SharedPreferences.Editor edit = _prefs.edit();
        if (color == NONE) {
            edit.remove(file.getAbsolutePath());
        } else {
            edit.putInt(file.getAbsolutePath(), color);
        }
        edit.apply();
    }

    /** Call after a rename or move so the file/folder keeps its color metadata. */
    public void move(final File from, final File to) {
        moveTree(from, to);
    }

    /** Remove the color for one file/folder. */
    public void remove(final File file) {
        if (file != null) {
            _prefs.edit().remove(file.getAbsolutePath()).apply();
        }
    }

    /** Remove the color for a file/folder and all descendants below it. */
    public void removeTree(final File root) {
        if (root == null) {
            return;
        }

        final String rootPath = root.getAbsolutePath();
        final String childPrefix = rootPath + File.separator;
        final SharedPreferences.Editor edit = _prefs.edit();
        boolean changed = false;
        for (final String key : _prefs.getAll().keySet()) {
            if (key.equals(rootPath) || key.startsWith(childPrefix)) {
                edit.remove(key);
                changed = true;
            }
        }
        if (changed) {
            edit.apply();
        }
    }

    /**
     * Move color metadata for a file/folder and all descendants. Existing metadata at the
     * destination is cleared first so an overwritten destination cannot leave stale colors.
     */
    public void moveTree(final File from, final File to) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }

        final String fromPath = from.getAbsolutePath();
        final String fromPrefix = fromPath + File.separator;
        final String toPath = to.getAbsolutePath();
        final String toPrefix = toPath + File.separator;
        final Map<String, Integer> moved = new HashMap<>();

        for (final Map.Entry<String, ?> entry : _prefs.getAll().entrySet()) {
            final String key = entry.getKey();
            if (key.equals(fromPath) || key.startsWith(fromPrefix)) {
                final Object value = entry.getValue();
                if (value instanceof Integer) {
                    final String suffix = key.substring(fromPath.length());
                    moved.put(toPath + suffix, (Integer) value);
                }
            }
        }

        final SharedPreferences.Editor edit = _prefs.edit();
        boolean changed = false;

        // Clear destination metadata first.
        for (final String key : _prefs.getAll().keySet()) {
            if (key.equals(toPath) || key.startsWith(toPrefix)) {
                edit.remove(key);
                changed = true;
            }
        }

        // Remove source metadata.
        for (final String key : _prefs.getAll().keySet()) {
            if (key.equals(fromPath) || key.startsWith(fromPrefix)) {
                edit.remove(key);
                changed = true;
            }
        }

        // Restore the moved entries at their new paths.
        for (final Map.Entry<String, Integer> entry : moved.entrySet()) {
            edit.putInt(entry.getKey(), entry.getValue());
            changed = true;
        }

        if (changed) {
            edit.apply();
        }
    }

    // ---------------------------------------------------------------- styling

    /**
     * Paint (or un-paint) a list row.
     *
     * @param row            The row's root view
     * @param color          A color from the palette, or NONE
     * @param darkBackground True if the app currently shows a dark theme
     */
    public static void applyToRow(
            final View row,
            final int color,
            final boolean darkBackground,
            final Drawable originalBackground
    ) {
        if (color == NONE) {
            // Always restore the ViewHolder's original background. This makes binding deterministic
            // even when RecyclerView reuses a row that was previously colored.
            row.setBackground(originalBackground);
            return;
        }

        final int fill = darkBackground
                ? ColorUtils.blendARGB(color, Color.BLACK, 0.70f)
                : ColorUtils.blendARGB(color, Color.WHITE, 0.82f);
        final int stripPx = Math.round(STRIP_WIDTH_DP * row.getResources().getDisplayMetrics().density);
        row.setBackground(new ColorNoteRowDrawable(fill, color, stripPx));
    }

    // ---------------------------------------------------------------- picking

    /** Shows a list of color names; applies the choice to all given files, then calls onChanged. */
    public static void showPicker(final Context context, final Collection<File> files, final FileColors store, final Runnable onChanged) {
        final String[] names = context.getResources().getStringArray(R.array.file_color_names);
        new AlertDialog.Builder(context)
                .setTitle(R.string.color)
                .setItems(names, (dialog, which) -> {
                    for (final File f : files) {
                        store.set(f, COLORS[which]);
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .show();
    }
}
