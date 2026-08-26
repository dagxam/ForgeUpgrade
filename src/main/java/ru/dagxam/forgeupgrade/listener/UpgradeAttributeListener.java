package ru.dagxam.forgeupgrade.listener;

import org.bukkit.event.Listener;

/**
 * Реальные обычные характеристики теперь хранятся непосредственно в ItemMeta
 * через AttributeModifier. Отдельная обработка каждого удара больше не нужна,
 * что исключает двойное увеличение урона и лишние проверки событий.
 */
public final class UpgradeAttributeListener implements Listener {
    // Класс сохранён для совместимости регистрации. Нагрузка отсутствует:
    // обработчиков событий здесь больше нет.
}
