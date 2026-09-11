package net.godlycow.org.essc.language;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HelpManager {

    private final EssentialsC plugin;
    private final MiniMessage mm;

    private String cLabel;
    private String cHeader;
    private String cBody;
    private String cDivider;
    private String cArgRequired;
    private String cArgOptional;

    public HelpManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.mm = plugin.getMiniMessage();
    }

    public void load(String defaultLanguage) {
        loadColors();
    }

    public void reload() {
        loadColors();
    }

    private void loadColors() {
        var cfg = plugin.getConfigManager();
        cLabel       = cfg.getHelpColorLabel();
        cHeader      = cfg.getHelpColorHeader();
        cBody        = cfg.getHelpColorBody();
        cDivider     = cfg.getHelpColorDivider();
        cArgRequired = cfg.getHelpColorArgRequired();
        cArgOptional = cfg.getHelpColorArgOptional();
    }

    public void sendHelp(CommandSender sender, String commandName, String sub) {
        PluginCommand command = plugin.getServer().getPluginCommand(commandName);

        String usage       = command != null && command.getUsage() != null       ? command.getUsage().trim()       : "/" + commandName;
        String permission  = command != null && command.getPermission() != null   ? command.getPermission().trim()   : null;
        String description = command != null && command.getDescription() != null  ? command.getDescription().trim()  : null;

        List<String> args = parseArgs(usage);
        List<Component> lines = buildHelp(commandName, usage, permission, description, args);

        if (sender instanceof Player) {
            for (Component line : lines) {
                sender.sendMessage(line);
            }
        } else {
            for (Component line : lines) {
                sender.sendMessage(Component.text(PlainTextComponentSerializer.plainText().serialize(line)));
            }
        }
    }

    private List<Component> buildHelp(String name, String usage, String permission, String description, List<String> args) {
        List<Component> lines = new ArrayList<>();

        String divider = cDivider + "───────────────────────────";

        lines.add(mm.deserialize(divider));
        lines.add(mm.deserialize(cHeader + "/" + name));

        if (description != null && !description.isBlank()) {
            lines.add(mm.deserialize(cBody + description));
        }

        lines.add(mm.deserialize(divider));

        lines.add(mm.deserialize(cLabel + "Usage  " + cBody + usage));

        if (!args.isEmpty()) {
            lines.add(mm.deserialize(cLabel + "Arguments"));
            for (String arg : args) {
                boolean required = arg.startsWith("<");
                String argColor = required ? cArgRequired : cArgOptional;
                String note = required ? "required" : "optional";
                lines.add(mm.deserialize("  " + argColor + arg + " " + cDivider + "— " + cBody + note));
            }
        }

        if (permission != null && !permission.isBlank()) {
            lines.add(mm.deserialize(cLabel + "Permission  " + cBody + permission));
        }

        lines.add(mm.deserialize(divider));

        return lines;
    }

    private List<String> parseArgs(String usage) {
        List<String> args = new ArrayList<>();
        if (usage == null || usage.isBlank()) {
            return args;
        }

        String[] tokens = usage.trim().split("\\s+");

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.startsWith("<") || token.startsWith("[")) {
                args.add(token);
            }
        }

        return args;
    }
}