package ui.controllers;

import java.util.Objects;

import io.github.palexdev.materialfx.controls.MFXComboBox;

import javafx.scene.input.MouseEvent;

import lombok.NonNull;

public class Commons {

    public static void setClicableComboBox(@NonNull MFXComboBox<?>... comboBoxes) {
        if (comboBoxes.length == 0) {
            throw new IllegalArgumentException("At least one combobox must be provided");
        }
        
        for (MFXComboBox<?> c : comboBoxes) {
            Objects.requireNonNull(c);
            c.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                c.requestFocus();
                if (!c.isShowing()) {
                    c.show();
                    event.consume();
                }
            });
        }
    }
}