(function(root) {
  root.LinearAssetBox = function(selectedLinearAsset, layerName, title, className, legendValues, showUnit, unit, complementaryPossible) {
    var legendTemplate = _.map(legendValues, function(value, idx) {
      return '<div class="legend-entry">' +
               '<div class="label">' + value + '</div>' +
               '<div class="symbol linear limit-' + idx + '" />' +
             '</div>';
    }).join('');


      var complementaryLinkCheckBox = complementaryPossible ? [
          '  <div class="panel-section roadLink-complementary-checkbox">',
          '<div class="check-box-container">' +
          '<input id="complementaryLinkCheckBox" type="checkbox" /> <lable>Näytä täydentävä geometria</lable>' +
          '</div>' +
          '</div>'
      ].join('') : '';

    var expandedTemplate = [
      '<div class="panel ' + layerName +'">',
      '  <header class="panel-header expanded">',
      '    ' + title + (showUnit ? ' ('+unit+')': ''),
      '  </header>',
      '  <div class="panel-section panel-legend limit-legend">',
            legendTemplate,
      '  </div>',
      complementaryLinkCheckBox,
      '</div>'].join('');

    var elements = {
      expanded: $(expandedTemplate)
    };

    var actions = [
      new ActionPanelBoxes.Tool('Select', ActionPanelBoxes.selectToolIcon, selectedLinearAsset),
      new ActionPanelBoxes.Tool('Cut', ActionPanelBoxes.cutToolIcon, selectedLinearAsset),
      new ActionPanelBoxes.Tool('Rectangle', ActionPanelBoxes.rectangleToolIcon, selectedLinearAsset),
      new ActionPanelBoxes.Tool('Polygon', ActionPanelBoxes.polygonToolIcon, selectedLinearAsset)
    ];

    var toolSelection = new ActionPanelBoxes.ToolSelection(actions);

    var editModeToggle = new EditModeToggleButton(toolSelection);
    var userRoles;

    var bindExternalEventHandlers = function() {
      eventbus.on('roles:fetched', function(roles) {
        userRoles = roles;
        if ((layerName != 'trafficVolume') && (_.contains(roles, 'operator') || (_.contains(roles, 'premium') && layerName != 'maintenanceRoad') ||
           (_.contains(roles, 'serviceRoadMaintainer') && layerName == 'maintenanceRoad'))) {
          toolSelection.reset();
          elements.expanded.append(toolSelection.element);
          elements.expanded.append(editModeToggle.element);
        }
      });
      eventbus.on('application:readOnly', function(readOnly) {
        elements.expanded.find('.panel-header').toggleClass('edit', !readOnly);
      });
    };

    bindExternalEventHandlers();

    elements.expanded.find('#complementaryLinkCheckBox').on('change', function (event) {
        if ($(event.currentTarget).prop('checked')) {
            eventbus.trigger('complementaryLinks:show');
        } else {
            if (applicationModel.isDirty()) {
                $(event.currentTarget).prop('checked', true);
                new Confirm();
            } else {
                eventbus.trigger('complementaryLinks:hide');
            }
        }
    });

    var element = $('<div class="panel-group simple-limit ' + className + 's"/>').append(elements.expanded).hide();

    function show() {
      if ((layerName == 'trafficVolume') || (editModeToggle.hasNoRolesPermission(userRoles) || (_.contains(userRoles, 'premium') && (layerName == 'maintenanceRoad')))) {
        editModeToggle.reset();
      } else {
        editModeToggle.toggleEditMode(applicationModel.isReadOnly());
      }
      element.show();
    }

    function hide() {
      element.hide();
    }

    return {
      title: title,
      layerName: layerName,
      element: element,
      show: show,
      hide: hide
    };
  };
})(this);

