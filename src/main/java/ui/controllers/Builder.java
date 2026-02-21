package ui.controllers;

import java.util.Arrays;
import java.util.Map;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Utility class for building and displaying various types of dialogs in the application using the MFX (MaterialFX) UI framework.
 * <p>
 * Provides static methods to create informational, warning, confirmation, error, and text input dialogs with customizable appearance and behavior.
 * </p>
 *
 */
public class Builder {

    /**
     * Represents the types of dialogs that can be displayed in the application.
     */
    @Accessors(chain = true, fluent = true)
    public static enum DialogType {
        INFO("Info", "mfx-info-dialog"),
        WARN("Warn", "mfx-warn-dialog"),
        CONFIRM("Confirm", "mfx-warn-dialog"),
        ERROR("Error", "mfx-error-dialog");

        @Getter
        private final String dialogType, styleClass;

        DialogType(String dialogType, String styleClass) {
            this.dialogType = dialogType;
            this.styleClass = styleClass;
        }

        public MFXFontIcon fontIcon() {
            return switch (this) {
                case INFO -> new MFXFontIcon("fas-circle-info", 18);
                case WARN, CONFIRM -> new MFXFontIcon("fas-circle-exclamation", 18);
                case ERROR -> new MFXFontIcon("fas-circle-xmark", 18);
            };
        }
    }

    /**
     * Creates and displays a customizable choice dialog with a header, content text, checkbox, and multiple selectable options.
     * The dialog allows the user to select one of the provided options or cancel the dialog.
     * 
     * @param owner       the parent {@link Stage} for the dialog window
     * @param ownerPane   the parent {@link Pane} to which the dialog is attached
     * @param title       the title of the dialog window
     * @param headerText  the header text displayed at the top of the dialog
     * @param contentText the main content text displayed in the dialog
     * @param checkBox    an {@link MFXCheckbox} to be displayed alongside the content
     * @param options     an array of option strings to be presented as buttons
     * @param pressed     an integer array of length 1; on selection, pressed[0] will be set to the index of the chosen option
     * @param cancelled   a boolean array of length 1; if the dialog is cancelled, cancelled[0] will be set to true
     * @return            the constructed {@link MFXStageDialog} instance
     */
    public static MFXStageDialog newChoiceDialog(Stage owner, Pane ownerPane, String title, String headerText, String contentText, MFXCheckbox checkBox, String[] options, int[] pressed, boolean cancelled[]) {
        Label headerLabel = new Label(headerText);
        Label contentLabel = new Label(contentText);

        GridPane gridPane = new GridPane();
        
        gridPane.setAlignment(Pos.CENTER_LEFT);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        gridPane.add(headerLabel, 0, 0, 2, 1);
        gridPane.add(contentLabel, 0, 1);

        if (checkBox != null) {
            checkBox.setMaxWidth(Double.MAX_VALUE);
            checkBox.setAlignment(Pos.CENTER_LEFT);
            gridPane.add(checkBox, 1, 1, 1, 1);    
        }

        gridPane.setStyle("-fx-font-size: 20px;");
        
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.setCenter(gridPane);
        dialogContent.setStyle("-fx-font-size: 20px;");
        dialogContent.setHeaderText(title);
        dialogContent.setMinSize(300, 200);
        dialogContent.setMaxSize(400, 300);

        MFXStageDialog dialog = newStageDialog(owner, ownerPane, title, dialogContent);
        dialog.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER -> dialog.close();
                case ESCAPE -> {
                    cancelled[0] = true;
                    dialog.close();
                }
                default -> {}
            }
            
        });
        dialogContent.addActions(
            Map.entry(new MFXButton("Cancel"), _ -> {
                cancelled[0] = true;
                dialog.close();
            })
        );
        var list = Arrays.asList(options);
        for (String s : list) {
            dialogContent.addActions(
                Map.entry(new MFXButton(s), _ -> {
                    pressed[0] = list.indexOf(s);
                    dialog.close();
                })
            );
        }

        return dialog;
    }

    /**
     * Creates and displays a modal dialog for text input with customizable title, header, content, and input field.
     * The dialog supports a maximum input length, and provides "Ok" and "Cancel" actions.
     * The dialog can be closed with the ENTER key (accepts input) or ESCAPE key (cancels input).
     *
     * @param owner      the owner {@link Stage} for the dialog window
     * @param ownerPane  the parent {@link Pane} to which the dialog is attached
     * @param title      the title of the dialog window
     * @param headerText the header text displayed at the top of the dialog
     * @param contentText the content text prompting the user for input
     * @param inputField the {@link MFXTextField} used for user input
     * @param maxLength  the maximum allowed length of the input; if <= 0, no limit is enforced
     * @param cancelled   a boolean array of length 1; set to true if the dialog is cancelled, false otherwise
     * @return the created {@link MFXStageDialog} instance
     */
    public static MFXStageDialog newTextInputDialog(Stage owner, Pane ownerPane, String title, String headerText, String contentText, MFXTextField inputField, int maxLength, boolean[] cancelled) {
        return newTextInputsDialog(owner, ownerPane, title, headerText, contentText, maxLength, cancelled, inputField);
    }

    public static MFXStageDialog newTextInputsDialog(Stage owner, Pane ownerPane, String title, String headerText, String contentText, int maxLength, boolean[] cancelled, MFXTextField... inputFields) {
        if (inputFields.length == 0) {
            throw new IllegalArgumentException("Input field array cannot be empty");
        }

        if (maxLength > 0) {
            for (MFXTextField inputField : inputFields) {
                inputField.textLimitProperty().set(maxLength);
            }
        }
        
        Label headerLabel = new Label(headerText);
        Label contentLabel = new Label(contentText);
        
        for (MFXTextField textField : inputFields) {
            GridPane.setHgrow(textField, Priority.ALWAYS);
            GridPane.setFillWidth(textField, true);
            textField.setMaxWidth(Double.MAX_VALUE);
        }

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setAlignment(Pos.CENTER_LEFT);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        gridPane.add(headerLabel, 0, 0, 2, 1);
        gridPane.add(contentLabel, 0, 1);

        int rowIndex = 1;
        for (MFXTextField inputField : inputFields) {
            gridPane.add(inputField, 1, rowIndex++);
        }

        gridPane.setStyle("-fx-font-size: 20px;");
        
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.setCenter(gridPane);
        dialogContent.setStyle("-fx-font-size: 20px;");
        dialogContent.setHeaderText(title);
        dialogContent.setMinSize(400, 200);
        dialogContent.setMaxSize(400, 400);

        MFXStageDialog dialog = newStageDialog(owner, ownerPane, title, dialogContent);
        dialog.setOnShown(_ -> inputFields[0].requestFocus());
        dialog.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case ENTER -> dialog.close();
                case ESCAPE -> {
                    cancelled[0] = true;
                    dialog.close();
                }
                default -> {}
            }
            
        });
        dialogContent.addActions(
            Map.entry(new MFXButton("Ok"), _ -> dialog.close()),
            Map.entry(new MFXButton("Cancel"), _ -> {
                cancelled[0] = true;
                dialog.close();
            })
        );

        return dialog;
    }

    public static MFXStageDialog newDialog(Stage stage, Pane ownerpane, DialogType type, boolean[] cancelled, StackTraceElement... stackTrace) {
        String s = String.join("\n", Arrays.toString(stackTrace));
        return newDialog(stage, ownerpane, s, type, cancelled);
    }

    public static MFXStageDialog newYesNoChoiceDialog(Stage owner, Pane ownerPane, String contentText, DialogType type, boolean[] selected) {
        MFXGenericDialog dialogContent = MFXGenericDialogBuilder.build()
            .setContentText(contentText)
            .makeScrollable(true)
            .get();
        dialogContent.setStyle("-fx-font-size: 24px;");
        
        MFXStageDialog dialog = newStageDialog(owner, ownerPane, type.dialogType() + " Dialog", dialogContent);
        dialogContent.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });
        dialogContent.addActions(
            Map.entry(new MFXButton("Yes"), _ -> {
                selected[0] = true;
                dialog.close();
            }),
            Map.entry(new MFXButton("No"), _ -> {
                dialog.close();
            })
        );

        dialogContent.setMinSize(400, 250);
        dialogContent.setMaxSize(600, 600);
        dialogContent.setHeaderIcon(type.fontIcon());
        dialogContent.setHeaderText(type.dialogType());
        convertDialogTo(dialogContent, dialog, type);

        return dialog;
    }

    /**
     * Creates and displays a new dialog window with customizable content and actions.
     *
     * @param owner      the parent {@link Stage} that owns the dialog
     * @param ownerPane  the {@link Pane} to which the dialog is attached
     * @param contentText the text content to display inside the dialog
     * @param type       the {@link DialogType} specifying the dialog's style and behavior
     * @param cancelled   a boolean array used to indicate if the dialog was cancelled (only for CONFIRM dialogs)
     * @return           the created {@link MFXStageDialog} instance
     * @throws IllegalArgumentException if {@code type} is {@code DialogType.CONFIRM} and {@code cancelled} is {@code null}
     */
    public static MFXStageDialog newDialog(Stage owner, Pane ownerPane, String contentText, DialogType type, boolean[] cancelled) {
        MFXButton okButton = new MFXButton("Ok");

        MFXGenericDialog dialogContent = MFXGenericDialogBuilder.build()
            .setContentText(contentText)
            .makeScrollable(true)
            .get();
        dialogContent.setStyle("-fx-font-size: 24px;");
        
        MFXStageDialog dialog = newStageDialog(owner, ownerPane, type.dialogType() + " Dialog", dialogContent);
        dialog.setOnShown(_ -> {
            dialog.centerOnScreen();
            okButton.requestFocus();
        });
        dialog.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (type == DialogType.CONFIRM) {
                    cancelled[0] = true;
                }
                dialog.close();
            }
        });

        if (type == DialogType.ERROR) {
            dialogContent.addActions(
                Map.entry(new MFXButton("Copy to Clipboard"), _ -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(contentText);
                    Clipboard.getSystemClipboard().setContent(content);
                    dialog.close();
                })
            );
        }
        dialogContent.addActions(Map.entry(okButton, _ -> dialog.close()));
        
        if (type == DialogType.CONFIRM || type == DialogType.WARN) {
            if (cancelled == null) {
                throw new IllegalArgumentException("Dialog type is CONFIRM, but cancelled is null");
            }
            dialogContent.addActions(
                Map.entry(new MFXButton("Cancel"), _ -> {
                    cancelled[0] = true;
                    dialog.close();
                })
            );
        }

        dialogContent.setMinSize(400, 300);
        dialogContent.setMaxSize(600, 600);
        dialogContent.setHeaderIcon(type.fontIcon());
        dialogContent.setHeaderText(type.dialogType());
        convertDialogTo(dialogContent, dialog, type);

        return dialog;
    }
    
    /**
     * Creates and configures a new {@link MFXStageDialog} with the specified owner, owner pane, title, and dialog content.
     *
     * @param owner         the {@link Stage} that owns the dialog
     * @param ownerPane     the {@link Pane} that acts as the owner node for the dialog
     * @param title         the title of the dialog window
     * @param dialogContent the {@link MFXGenericDialog} content to display in the dialog
     * @return a configured {@link MFXStageDialog} instance
     */
    private static MFXStageDialog newStageDialog(Stage owner, Pane ownerPane, String title, MFXGenericDialog dialogContent) {
        return MFXGenericDialogBuilder.build(dialogContent)
            .toStageDialogBuilder()
            .initOwner(owner)
            .initModality(Modality.APPLICATION_MODAL)
            .setDraggable(true)
            .setTitle(title)
            .setOwnerNode(ownerPane)
            .setScrimPriority(ScrimPriority.WINDOW)
            .setScrimOwner(true)
            .get();
    }

    /**
     * Updates the style classes of the given {@code MFXGenericDialog} based on the specified {@code DialogType}.
     * <p>
     * Removes any existing style classes that match a certain condition (as defined by {@code Builder::match}),
     * and if a {@code DialogType} is provided, adds its associated style class to the dialog content.
     *
     * @param dialogContent the dialog content whose style classes will be updated
     * @param dialog the parent dialog window (not modified by this method)
     * @param type the type of dialog, used to determine which style class to add; if {@code null}, no style is added
     */
    private static void convertDialogTo(MFXGenericDialog dialogContent, MFXStageDialog dialog, DialogType type) {
		dialogContent.getStyleClass().removeIf(Builder::match);

		if (type != null) {
			dialogContent.getStyleClass().add(type.styleClass());
        }
	}

    /**
     * Checks if the given string matches any of the dialogTypes of the {@link DialogType} enum values.
     *
     * @param s the string to check for a match against the {@code DialogType} values
     * @return {@code true} if the string matches any {@code DialogType} value; {@code false} otherwise
     */
    private static boolean match(String s) {
        return Arrays.stream(DialogType.values())
            .anyMatch(value -> value.dialogType.equals(s));
    }
}
