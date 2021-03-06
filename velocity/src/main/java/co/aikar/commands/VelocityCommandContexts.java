/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.commands;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import co.aikar.commands.annotation.Optional;
import co.aikar.commands.contexts.OnlinePlayer;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.format.TextFormat;

public class VelocityCommandContexts extends CommandContexts<VelocityCommandExecutionContext> {

    VelocityCommandContexts(ProxyServer server, CommandManager manager) {
        super(manager);
        registerContext(OnlinePlayer.class, (c) -> {
            Player proxiedPlayer = ACFVelocityUtil.findPlayerSmart(server, c.getIssuer(), c.popFirstArg());
            if (proxiedPlayer == null) {
                if (c.hasAnnotation(Optional.class)) {
                    return null;
                }
                throw new InvalidCommandArgument(false);
            }
            return new OnlinePlayer(proxiedPlayer);
        });
        registerIssuerAwareContext(CommandSource.class, VelocityCommandExecutionContext::getSender);
        registerIssuerAwareContext(Player.class, (c) -> {
            Player proxiedPlayer = c.getSender() instanceof Player ? (Player) c.getSender() : null;
            if (proxiedPlayer == null && !c.hasAnnotation(Optional.class)) {
                throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE, false);
            }
            return proxiedPlayer;
        });

        registerContext(TextFormat.class, c -> {
            String first = c.popFirstArg();
            Stream<TextFormat> colors = Stream.of(TextColor.values());
            if (!c.hasFlag("colorsonly")) {
                colors = Stream.concat(colors, Stream.of(TextDecoration.values()));
            }
            String filter = c.getFlagValue("filter", (String) null);
            if (filter != null) {
                filter = ACFUtil.simplifyString(filter);
                String finalFilter = filter;
                colors = colors.filter(color -> finalFilter.equals(ACFUtil.simplifyString(color.toString())));
            }

            TextColor match = ACFUtil.simpleMatch(TextColor.class, first);
            if (match == null) {
                String valid = colors.map(color -> "<c2>" + ACFUtil.simplifyString(color.toString()) + "</c2>")
                        .collect(Collectors.joining("<c1>,</c1> "));

                throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_ONE_OF, "{valid}", valid);
            }
            return match;
        });
    }
}
