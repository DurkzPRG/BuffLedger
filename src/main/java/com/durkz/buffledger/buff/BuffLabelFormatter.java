package com.durkz.buffledger.buff;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BuffLabelFormatter {

    private static final Pattern FOOD_TIER = Pattern.compile("_T([1-5])$");
    private static final Pattern POTION_TIER = Pattern.compile("_(Lesser|Small|Large|Greater)$");
    private static final Pattern SIZED_BUFF = Pattern.compile(
            "^(Speed|Fire_Resistance|Poison_Resistance|Resistance|Water_Breathing)_(Tiny|Small|Medium)$"
    );

    private BuffLabelFormatter() {}

    static String fromEffectId(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return "effect";
        }

        if (effectId.startsWith("Potion_Health_Regen")) {
            return "HP Regen" + potionTier(effectId, "Potion_Health_Regen");
        }
        if (effectId.startsWith("Potion_Health_Instant")) {
            return "HP Heal" + potionTier(effectId, "Potion_Health_Instant");
        }
        if (effectId.startsWith("Potion_Stamina_Regen")) {
            return "Stamina Regen" + potionTier(effectId, "Potion_Stamina_Regen");
        }
        if (effectId.startsWith("Potion_Stamina_Instant")) {
            return "Stamina Heal" + potionTier(effectId, "Potion_Stamina_Instant");
        }
        if (effectId.startsWith("Potion_Signature_Regen")) {
            return "Signature Regen" + potionTier(effectId, "Potion_Signature_Regen");
        }
        if (effectId.startsWith("Potion_Morph_")) {
            return "Morph " + effectId.substring("Potion_Morph_".length()).replace('_', ' ');
        }

        String nepLabel = notEnoughPotionsLabel(effectId);
        if (nepLabel != null) {
            return nepLabel;
        }

        if (effectId.startsWith("Meat_Buff_")) {
            return "Max HP " + foodTier(effectId);
        }
        if (effectId.startsWith("FruitVeggie_Buff_")) {
            return "Stamina " + foodTier(effectId);
        }
        if (effectId.startsWith("HealthRegen_Buff_")) {
            return "HP Regen " + foodTier(effectId);
        }
        if (effectId.startsWith("Food_Stamina_Boost_")) {
            return "Stamina Boost " + sizeLabel(effectId.substring("Food_Stamina_Boost_".length()));
        }
        if (effectId.startsWith("Food_Health_Boost_")) {
            return "Max HP Boost " + sizeLabel(effectId.substring("Food_Health_Boost_".length()));
        }
        if (effectId.startsWith("Food_Health_Regen_")) {
            return "HP Regen " + sizeLabel(effectId.substring("Food_Health_Regen_".length()));
        }
        if (effectId.startsWith("Food_Stamina_Regen_")) {
            return "Stamina Regen " + sizeLabel(effectId.substring("Food_Stamina_Regen_".length()));
        }
        if (effectId.startsWith("Food_Instant_Heal_")) {
            return "Instant Heal " + sizeLabel(effectId.substring("Food_Instant_Heal_".length()));
        }

        String sizedBuff = sizedBuffLabel(effectId);
        if (sizedBuff != null) {
            return sizedBuff;
        }

        if (effectId.startsWith("Juice_")) {
            return juiceLabel(effectId);
        }

        return compactHumanize(effectId);
    }

    private static String notEnoughPotionsLabel(String effectId) {
        return switch (effectId) {
            case "Potion_Speed" -> "Speed";
            case "Potion_Speed_Greater" -> "Speed (Greater)";
            case "Potion_Strength" -> "Strength";
            case "Potion_Resistance" -> "Resistance";
            case "Potion_Fire_Resist" -> "Fire Resist";
            case "Potion_Water_Breathe" -> "Water Breathing";
            case "Potion_Water_Breathe_Greater" -> "Water Breathing (Greater)";
            case "Potion_Life_Steal" -> "Life Steal";
            case "Potion_Thorns" -> "Thorns";
            case "Potion_Fall_Damage" -> "Fall Protection";
            case "Potion_Jump_Boost" -> "Jump Boost";
            case "Potion_Levitation" -> "Levitation";
            case "Potion_Invisibility" -> "Invisibility";
            case "Potion_Invisibility_Splash" -> "Invisibility (Splash)";
            case "Potion_Night_Vision" -> "Night Vision";
            case "Potion_Scale_Grow" -> "Giant Form";
            case "Potion_Scale_Shrink" -> "Tiny Form";
            case "Potion_Stamina_Plus" -> "Max Stamina";
            case "Potion_Stamina_Plus_Greater" -> "Max Stamina (Greater)";
            case "Potion_Poison" -> "Poison";
            case "Potion_Freeze" -> "Frozen";
            case "Potion_Healing_Splash" -> "Heal (Splash)";
            case "Teleport_Departure" -> "Teleporting";
            case "Teleport_Arrival" -> "Teleport";
            default -> null;
        };
    }

    private static String sizedBuffLabel(String effectId) {
        Matcher matcher = SIZED_BUFF.matcher(effectId);
        if (!matcher.matches()) {
            return null;
        }
        String base = switch (matcher.group(1)) {
            case "Fire_Resistance" -> "Fire Resist";
            case "Poison_Resistance" -> "Poison Resist";
            case "Water_Breathing" -> "Water Breathing";
            default -> matcher.group(1).replace('_', ' ');
        };
        return base + " (" + matcher.group(2) + ")";
    }

    private static String juiceLabel(String effectId) {
        return switch (effectId) {
            case "Juice_Health_Instant" -> "Juice HP Heal";
            case "Juice_Health_Regen" -> "Juice HP Regen";
            case "Juice_Stamina_Instant" -> "Juice Stamina Heal";
            default -> "Juice " + sizeLabel(effectId.substring("Juice_".length()));
        };
    }

    private static String potionTier(String effectId, String prefix) {
        String suffix = effectId.substring(prefix.length());
        if (suffix.isEmpty()) {
            return "";
        }
        if (suffix.startsWith("_")) {
            suffix = suffix.substring(1);
        }
        if (suffix.isEmpty()) {
            return "";
        }
        return " (" + titleCase(suffix.replace('_', ' ')) + ")";
    }

    private static String foodTier(String effectId) {
        Matcher matcher = FOOD_TIER.matcher(effectId);
        if (matcher.find()) {
            return "T" + matcher.group(1);
        }
        return "";
    }

    private static String sizeLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return titleCase(raw.replace('_', ' '));
    }

    private static String compactHumanize(String raw) {
        String cleaned = raw.replace('_', ' ').trim();
        Matcher potion = POTION_TIER.matcher(cleaned);
        if (potion.find()) {
            String tier = potion.group(1);
            String base = cleaned.substring(0, potion.start()).trim();
            return base + " (" + tier + ")";
        }
        Matcher food = Pattern.compile(" T([1-5])$").matcher(cleaned);
        if (food.find()) {
            return cleaned.substring(0, food.start()) + " T" + food.group(1);
        }
        return cleaned;
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    out.append(part.substring(1));
                }
            }
        }
        return out.toString();
    }
}
