package util;

import javafx.css.PseudoClass;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.function.Predicate;

public class TableRowHighlighter {

    private static final PseudoClass UNREAD =
            PseudoClass.getPseudoClass("unread");

    private static final PseudoClass READ =
            PseudoClass.getPseudoClass("read");

    public static <T> void apply(TableView<T> table, Predicate<T> isUnread) {

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    pseudoClassStateChanged(UNREAD, false);
                    pseudoClassStateChanged(READ, false);
                    return;
                }

                boolean unread = isUnread.test(item);

                pseudoClassStateChanged(UNREAD, unread);
                pseudoClassStateChanged(READ, !unread);
            }
        });
    }
}
