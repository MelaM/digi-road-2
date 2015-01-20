(function(root) {

  var ValidationErrorLabel = function() {
    var element = $('<span class="validation-error">Pakollisia tietoja puuttuu</span>');

    var updateVisibility = function() {
      if (selectedAssetModel.isDirty() && selectedAssetModel.requiredPropertiesMissing()) {
        element.show();
      } else {
        element.hide();
      }
    };

    updateVisibility();

    eventbus.on('asset:moved assetPropertyValue:changed', function() {
      updateVisibility();
    }, this);

    return {
      element: element
    };
  };

  var SaveButton = function() {
    var element = $('<button />').addClass('save btn btn-primary').text('Tallenna').click(function() {
      element.prop('disabled', true);
      selectedAssetModel.save();
    });
    var updateStatus = function() {
      if (selectedAssetModel.isDirty() && !selectedAssetModel.requiredPropertiesMissing()) {
        element.prop('disabled', false);
      } else {
        element.prop('disabled', true);
      }
    };

    updateStatus();

    eventbus.on('asset:moved assetPropertyValue:changed', function() {
      updateStatus();
    }, this);

    return {
      element: element
    };
  };

  var CancelButton = function() {
    var element = $('<button />').prop('disabled', !selectedAssetModel.isDirty()).addClass('cancel btn btn-secondary').text('Peruuta').click(function() {
      $("#feature-attributes").empty();
      selectedAssetModel.cancel();
    });

    eventbus.on('asset:moved assetPropertyValue:changed', function() {
      element.prop('disabled', false);
    }, this);

    return {
      element: element
    };
  };

  root.AssetForm = {
    initialize: function(backend, applicationModel) {
      var enumeratedPropertyValues = null;
      var streetViewHandler;
      var activeLayer = 'asset';

      var isReadOnly = function() {
        return applicationModel.isReadOnly() || (selectedAssetModel.exists() && selectedAssetModel.isReadOnly());
      };

      var renderAssetForm = function() {
        if (activeLayer !== 'asset') {
          return;
        }
        var container = $("#feature-attributes").empty();
        var header = busStopHeader();
        var wrapper = $('<div />').addClass('wrapper edit-mode');
        streetViewHandler = getStreetView();
        wrapper.append(streetViewHandler.render())
          .append($('<div />').addClass('form form-horizontal form-dark').attr('role', 'form').append(getAssetForm()));
        var featureAttributesElement = container.append(header).append(wrapper);
        addDatePickers();

        var saveBtn = new SaveButton();
        var cancelBtn = new CancelButton();
        var validationErrorLabel = new ValidationErrorLabel();

        featureAttributesElement.append($('<footer />')
            .addClass('mass-transit-stop')
            .addClass('form-controls')
            .append(validationErrorLabel.element)
            .append(saveBtn.element)
            .append(cancelBtn.element));

        if (isReadOnly()) {
          $('#feature-attributes .form-controls').hide();
          wrapper.addClass('read-only');
          wrapper.removeClass('edit-mode');
        }

        function busStopHeader(asset) {
          var buttons = $('<div/>').addClass('mass-transit-stop').addClass('form-controls')
            .append(new ValidationErrorLabel().element)
            .append(new SaveButton().element)
            .append(new CancelButton().element);

          var header = $('<header/>');

          if (_.isNumber(selectedAssetModel.get('externalId'))) {
            header.append('<span>Valtakunnallinen ID: ' + selectedAssetModel.get('externalId') + '</span>');
          } else {
            header.append('<span>Uusi pys&auml;kki</span>');
          }
          header.append(buttons);
          return header;
        }
      };

      var getStreetView = function() {
        var model = selectedAssetModel;
        var render = function() {
          var wgs84 = proj4('EPSG:3067', 'WGS84', [model.get('lon'), model.get('lat')]);
          return $(streetViewTemplate({
            wgs84X: wgs84[0],
            wgs84Y: wgs84[1],
            heading: (model.get('validityDirection') === validitydirections.oppositeDirection ? model.get('bearing') - 90 : model.get('bearing') + 90),
            protocol: location.protocol
          })).addClass('street-view');
        };

        var update = function(){
          $('.street-view').empty().append(render());
        };

        return {
          render: render,
          update: update
        };
      };

      var addDatePickers = function () {
        var $validFrom = $('#ensimmainen_voimassaolopaiva');
        var $validTo = $('#viimeinen_voimassaolopaiva');
        if ($validFrom.length > 0 && $validTo.length > 0) {
          dateutil.addDependentDatePickers($validFrom, $validTo);
        }
      };

      var createWrapper = function(property) {
        var wrapper = createFormRowDiv();
        wrapper.append(createLabelElement(property));
        return wrapper;
      };

      var createFormRowDiv = function() {
        return $('<div />').addClass('form-group');
      };

      var createLabelElement = function(property) {
        var label = $('<label />').addClass('control-label').text(property.localizedName);
        if (property.required) {
          label.addClass('required');
        }
        return label;
      };

      var readOnlyHandler = function(property){
        var outer = createFormRowDiv();
        var propertyVal = _.isEmpty(property.values) === false ? property.values[0].propertyDisplayValue : '';
        if (property.propertyType === 'read_only_text') {
          outer.append($('<p />').addClass('form-control-static asset-log-info').text(property.localizedName + ': ' + propertyVal));
        } else {
          outer.append(createLabelElement(property));
          outer.append($('<p />').addClass('form-control-static').text(propertyVal));
        }
        return outer;
      };

      var textHandler = function(property){
        return createWrapper(property).append(createTextElement(isReadOnly(), property));
      };

      var createTextElement = function(readOnly, property) {
        var element;
        var elementType;

        if (readOnly) {
          elementType = $('<p />').addClass('form-control-static');
          element = elementType;

          if (property.values[0]) {
            element.text(property.values[0].propertyDisplayValue);
          } else {
            element.addClass('undefined').html('Ei m&auml;&auml;ritetty');
          }
        } else {
          elementType = property.propertyType === 'long_text' ?
            $('<textarea />').addClass('form-control') : $('<input type="text"/>').addClass('form-control');
          element = elementType.bind('input', function(target){
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: target.currentTarget.value }]);
          });

          if(property.values[0]) {
            element.val(property.values[0].propertyDisplayValue);
          }
        }

        return element;
      };

      var singleChoiceHandler = function(property, choices){
        return createWrapper(property).append(createSingleChoiceElement(isReadOnly(), property, choices));
      };

      var createSingleChoiceElement = function(readOnly, property, choices) {
        var element;
        var enumValues = _.find(choices, function(choice){
          return choice.publicId === property.publicId;
        }).values;

        if (readOnly) {
          element = $('<p />').addClass('form-control-static');

          if (property.values && property.values[0]) {
            element.text(property.values[0].propertyDisplayValue);
          } else {
            element.html('Ei tiedossa');
          }
        } else {
          element = $('<select />').addClass('form-control').change(function(x){
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: x.currentTarget.value }]);
          });

          element = _.reduce(enumValues, function(element, value) {
            var option = $('<option>').text(value.propertyDisplayValue).attr('value', value.propertyValue);
            element.append(option);
            return element;
          }, element);

          if(property.values && property.values[0]) {
            element.val(property.values[0].propertyValue);
          } else {
            element.val('99');
          }
        }

        return element;
      };

      var directionChoiceHandler = function(property) {
        if (!isReadOnly()) {
          return createWrapper(property).append(createDirectionChoiceElement());
        }
      };

      var createDirectionChoiceElement = function() {
        return $('<button />').addClass('btn btn-secondary btn-block').text('Vaihda suuntaa').click(function() {
          selectedAssetModel.switchDirection();
          streetViewHandler.update();
        });
      };

      var dateHandler = function(property){
        return createWrapper(property).append(createDateElement(isReadOnly(), property));
      };

      var notificationHandler = function(property) {
        if (property.enabled) {
          var row = createFormRowDiv().addClass('form-notification');
          row.append($('<p />').text(property.text));
          return row;
        } else {
          return [];
        }
      };

      var createDateElement = function(readOnly, property) {
        var element;

        if (readOnly) {
          element = $('<p />').addClass('form-control-static');

          if (property.values[0]) {
            element.text(dateutil.iso8601toFinnish(property.values[0].propertyDisplayValue));
          } else {
            element.addClass('undefined').html('Ei m&auml;&auml;ritetty');
          }
        } else {
          element = $('<input type="text"/>').addClass('form-control').attr('id', property.publicId).on('keyup datechange', _.debounce(function(target){
            // tab press
            if(target.keyCode === 9){
              return;
            }
            var propertyValue = _.isEmpty(target.currentTarget.value) ? '' : dateutil.finnishToIso8601(target.currentTarget.value);
            selectedAssetModel.setProperty(property.publicId, [{ propertyValue: propertyValue }]);
          }, 500));

          if (property.values[0]) {
            element.val(dateutil.iso8601toFinnish(property.values[0].propertyDisplayValue));
          }
        }

        return element;
      };

      var multiChoiceHandler = function(property, choices){
        return createWrapper(property).append(createMultiChoiceElement(isReadOnly(), property, choices));
      };

      var createMultiChoiceElement = function(readOnly, property, choices) {
        var element;
        var currentValue = _.cloneDeep(property);
        var enumValues = _.chain(choices)
          .filter(function(choice){
            return choice.publicId === property.publicId;
          })
          .flatten('values')
          .filter(function(x){
            return x.propertyValue !== '99';
          }).value();

        if (readOnly) {
          element = $('<ul />');
        } else {
          element = $('<div />');
        }

        element.addClass('choice-group');

        element = _.reduce(enumValues, function(element, value) {
          value.checked = _.any(currentValue.values, function (prop) {
            return prop.propertyValue === value.propertyValue;
          });

          if (readOnly) {
            if (value.checked) {
              var item = $('<li />');
              item.text(value.propertyDisplayValue);

              element.append(item);
            }
          } else {
            var container = $('<div class="checkbox" />');
            var input = $('<input type="checkbox" />').change(function (evt) {
              value.checked = evt.currentTarget.checked;
              var values = _.chain(enumValues)
                .filter(function (value) {
                  return value.checked;
                })
                .map(function (value) {
                  return { propertyValue: parseInt(value.propertyValue, 10) };
                })
                .value();
              if (_.isEmpty(values)) { values.push({ propertyValue: 99 }); }
              selectedAssetModel.setProperty(property.publicId, values);
            });

            input.prop('checked', value.checked);

            var label = $('<label />').text(value.propertyDisplayValue);
            element.append(container.append(label.append(input)));
          }

          return element;
        }, element);

        return element;
      };

      var floatingStatus = function(selectedAssetModel) {
        return [{
          propertyType: 'notification',
          enabled: selectedAssetModel.get('floating'),
          text: 'Kadun tai tien geometria on muuttunut, tarkista ja korjaa pysäkin sijainti.'
        }];
      };

      var getAssetForm = function() {
        var properties = selectedAssetModel.getProperties();
        var contents = _.first(properties, 2)
          .concat(floatingStatus(selectedAssetModel))
          .concat(_.rest(properties, 2));
        var components =_.map(contents, function(feature){
          feature.localizedName = window.localizedStrings[feature.publicId];
          var propertyType = feature.propertyType;
          if (propertyType === "text" || propertyType === "long_text") {
            return textHandler(feature);
          } else if (propertyType === "read_only_text" || propertyType === 'read-only') {
            return readOnlyHandler(feature);
          } else if (feature.publicId === 'vaikutussuunta') {
            return directionChoiceHandler(feature);
          } else if (propertyType === "single_choice") {
            return singleChoiceHandler(feature, enumeratedPropertyValues);
          } else if (feature.propertyType === "multiple_choice") {
            return multiChoiceHandler(feature, enumeratedPropertyValues);
          } else if (propertyType === "date") {
            return dateHandler(feature);
          } else if (propertyType === 'notification') {
            return notificationHandler(feature);
          }  else {
            feature.propertyValue = 'Ei toteutettu';
            return $(featureDataTemplateNA(feature));
          }
        });

        return $('<div />').append(components);
      };

      var streetViewTemplate  = _.template(
          '<a target="_blank" href="<%= protocol %>//maps.google.com/?ll=<%= wgs84Y %>,<%= wgs84X %>&cbll=<%= wgs84Y %>,<%= wgs84X %>&cbp=12,<%= heading %>.09,,0,5&layer=c&t=m">' +
          '<img alt="Google StreetView-n&auml;kym&auml;" src="http://maps.googleapis.com/maps/api/streetview?key=AIzaSyBh5EvtzXZ1vVLLyJ4kxKhVRhNAq-_eobY&size=360x180&location=<%= wgs84Y %>' +
          ', <%= wgs84X %>&fov=110&heading=<%= heading %>&pitch=-10&sensor=false">' +
          '</a>');

      var featureDataTemplateNA = _.template('<div class="formAttributeContentRow">' +
        '<div class="formLabels"><%= localizedName %></div>' +
        '<div class="featureAttributeNA"><%= propertyValue %></div>' +
        '</div>');

      var closeAsset = function() {
        $("#feature-attributes").html('');
        dateutil.removeDatePickersFromDom();
      };

      eventbus.on('asset:modified', function(){
        renderAssetForm();
      });

      eventbus.on('layer:selected', function(layer) {
        activeLayer = layer;
        closeAsset();
      });

      eventbus.on('asset:closed', closeAsset);

      eventbus.on('enumeratedPropertyValues:fetched', function(values) {
        enumeratedPropertyValues = values;
      });

      eventbus.on('asset:moved', function() {
        streetViewHandler.update();
      });

      backend.getEnumeratedPropertyValues(10);
    }
  };
})(this);

