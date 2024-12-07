package me.chimkenu.bopbipEssentials.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RegexConstraint;

@Modmenu(modId = "bopbip-essentials")
@Config(name = "bopbip-config", wrapperName = "BopbipConfig")
public class BopbipConfigModel {
    @RegexConstraint("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)")
    public String githubLink = "https://github.com/chimkenu/bopbip-world.git";

    @RegexConstraint("/^\\w+$/")
    public String githubKey = "placeholder";
}
