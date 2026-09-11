package net.godlycow.org.essc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public final class ComponentHelper {

    private ComponentHelper() {}

    public static Component noItalic(Component component) {
        if (component == null) return Component.empty();
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> noItalic(List<Component> components) {
        if (components == null) return new ArrayList<>();
        List<Component> result = new ArrayList<>(components.size());
        for (Component c : components) {
            result.add(noItalic(c));
        }
        return result;
    }
}