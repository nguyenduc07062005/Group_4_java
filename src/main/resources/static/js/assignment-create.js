(() => {
    const bindAssignmentForm = () => {
        const semesterField = document.querySelector("[data-semester-select]");
        const courseField = document.querySelector("[data-course-select]");
        const gradingModeField = document.querySelector("[data-grading-mode-select]");
        const assignmentTypeField = document.querySelector("[data-assignment-type-select]");
        const weekNumberGroup = document.getElementById("week-number-group");
        const oopSections = Array.from(document.querySelectorAll("[data-oop-section]"));
        const oopFields = Array.from(document.querySelectorAll("[data-oop-field]"));
        const oopRuleField = document.getElementById("oopRuleConfig");

        if (!semesterField || !courseField || semesterField.dataset.flowBound === "true") {
            return;
        }

        const syncCourseOptions = () => {
            const semesterId = semesterField.value;
            const currentCourseValue = courseField.value;
            let selectedStillVisible = currentCourseValue === "";

            Array.from(courseField.querySelectorAll("optgroup")).forEach((group) => {
                const groupSemesterId = group.dataset.semesterGroup;
                const visible = !semesterId || groupSemesterId === semesterId;
                group.hidden = !visible;

                Array.from(group.querySelectorAll("option")).forEach((option) => {
                    const optionVisible = !semesterId || option.dataset.semesterId === semesterId;
                    option.hidden = !optionVisible;
                    if (option.value === currentCourseValue && optionVisible) {
                        selectedStillVisible = true;
                    }
                });
            });

            if (!selectedStillVisible) {
                courseField.value = "";
            }
        };

        const syncAssignmentType = () => {
            const assignmentType = assignmentTypeField ? assignmentTypeField.value : "CUSTOM";
            const isWeekly = assignmentType === "WEEKLY";

            if (weekNumberGroup) {
                weekNumberGroup.hidden = !isWeekly;
                const weekField = weekNumberGroup.querySelector("input");
                if (weekField) {
                    weekField.disabled = !isWeekly;
                    if (!isWeekly) {
                        weekField.value = "";
                    }
                }
            }
        };

        const syncOopFields = () => {
            const isOopMode = gradingModeField && gradingModeField.value === "OOP";

            oopSections.forEach((section) => {
                section.hidden = !isOopMode;
            });

            oopFields.forEach((field) => {
                field.disabled = !isOopMode;
            });

            if (!isOopMode && oopRuleField) {
                oopRuleField.value = "";
            }
        };

        semesterField.addEventListener("change", syncCourseOptions);
        semesterField.addEventListener("input", syncCourseOptions);

        if (assignmentTypeField) {
            assignmentTypeField.addEventListener("change", syncAssignmentType);
            assignmentTypeField.addEventListener("input", syncAssignmentType);
        }

        if (gradingModeField) {
            gradingModeField.addEventListener("change", syncOopFields);
            gradingModeField.addEventListener("input", syncOopFields);
        }

        window.addEventListener("pageshow", () => {
            syncCourseOptions();
            syncAssignmentType();
            syncOopFields();
        });

        semesterField.dataset.flowBound = "true";
        syncCourseOptions();
        syncAssignmentType();
        syncOopFields();
        requestAnimationFrame(() => {
            syncCourseOptions();
            syncAssignmentType();
            syncOopFields();
        });
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bindAssignmentForm, {once: true});
    } else {
        bindAssignmentForm();
    }
})();
