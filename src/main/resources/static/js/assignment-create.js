(() => {
    const gradingModeField = document.getElementById("gradingMode");
    const oopSection = document.querySelector("[data-oop-section]");
    const oopFields = Array.from(document.querySelectorAll("[data-oop-field]"));
    const oopRuleField = document.getElementById("oopRuleConfig");

    if (!gradingModeField || !oopSection) {
        return;
    }

    const syncOopFields = () => {
        const isOopMode = gradingModeField.value === "OOP";
        oopSection.hidden = !isOopMode;
        oopFields.forEach((field) => {
            field.disabled = !isOopMode;
        });

        if (!isOopMode && oopRuleField) {
            oopRuleField.value = "";
        }
    };

    gradingModeField.addEventListener("change", syncOopFields);
    syncOopFields();
})();
