(function ($) {
  "use strict";

  let intervalID = setInterval(function () {
    if (!document.querySelector('[data-type="Editable"]')) {
      return;
    } else {
      clearInterval(intervalID);
    }

    document.querySelectorAll('[data-type="Editable"]').forEach(async function (editable) {
      const componentPath = editable.dataset.path;

      const response = await fetch(`${componentPath}.json`);
      const json = await response.json();

      //if (json['sling:resourceType'] === 'wcm/foundation/components/responsivegrid') {
      if (json["sling:resourceType"].includes("/components/container")) {
        // It's a Container component; do nothing
      } else if (!json.status?.trim()) {
        // No status property, set to `status--none`; only implement the CSS if this is required
        editable.classList.add("status--none");
      } else {
        // Has status property, use its value as part of the CSS class name
        const status = json.status.toLowerCase().replace(/[^a-z0-9]+/g, "-");
        editable.classList.add(`status--${status}`);
      }
    });
  }, 500); // Check every 1/2 second
})(jQuery);
