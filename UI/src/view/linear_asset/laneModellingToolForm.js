(function(root) {
  var DynamicField = function (fieldSettings, isDisabled) {
    var me = this;
    me.element = undefined;

    me.disabled = function() { return isDisabled ? 'disabled' : '';};
    me.required = function() { return _.isUndefined(fieldSettings.required) ? '' : 'required';};

    me.hasDefaultValue = function(){
      return !_.isUndefined(fieldSettings.defaultValue);
    };

    me.createPropertyValue = function(values){
      return {
        publicId: fieldSettings.publicId,
        propertyType: fieldSettings.type,
        required : fieldSettings.required,
        values: values
      };
    };

    me.getPropertyValue = function(){
      var value = me.getValue();
      return me.createPropertyValue([{ value: value }]);
    };

    me.getPropertyDefaultValue = function(){
      return me.createPropertyValue([{ value: fieldSettings.defaultValue}]);
    };

    me.emptyPropertyValue = function(){
      return me.createPropertyValue([]);
    };

    me.isValid = function(){
      return !me.isRequired() || me.isRequired() && me.hasValue();
    };

    me.compare = function(propertyValueA, propertyValueB){
      return _.isEqual(_.head(propertyValueA.values).value, _.head(propertyValueB.values).value);
    };

    me.hasValue = function(){
      return !_.isEmpty(me.getValue());
    };

    me.getValue = function() {
      return $(me.element).find('input').val();
    };

    me.setValue = function() {};

    me.hasValidValue = function() {};

    me.isRequired = function() {
      return fieldSettings.required ? fieldSettings.required : false;
    };

    me.viewModeRender = function (field, propertyValues) {
      var value = _.head(propertyValues, function(propertyValue) { return propertyValue.value ; });
      var unit = field.unit ? ' ' + field.unit : '';
      var _value = value ? value.value + unit: '-';

      return $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <p class="form-control-static">' + _value  + '</p>' +
        '</div>'
      );
    };

    me.editModeRender = function (currentValue, sideCode, setValue, getValue){};

    me.setSelectedValue = function(setValue, getValue){

      var currentPropertyValue = me.hasValue() ?  me.getPropertyValue() : (me.hasDefaultValue() ? me.getPropertyDefaultValue() : me.emptyPropertyValue());

      var properties = _.filter(getValue(currentLane), function(property){ return property.publicId !== currentPropertyValue.publicId; });
      properties.push(currentPropertyValue);
      setValue(currentLane, {properties: properties});
    };
  };

  var TextualField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className =  assetTypeConfiguration.className;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : '';

      me.element = $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <input type="text" fieldType = "' + field.type + '" '+ me.required() +' name="' + field.publicId + '" class="form-control ' + className + '" ' + me.disabled()  + ' value="' + _value + '" >' +
        '</div>');

      if (!isDisabled && me.hasDefaultValue() && !value)
        me.setSelectedValue(setValue, getValue);

      me.element.find('input[type=text]').on('keyup', function(){
        me.setSelectedValue(setValue, getValue);
      });
      return me.element;
    };
  };

  var TextualLongField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = assetTypeConfiguration.className;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : '';

      me.element = $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <textarea fieldType = "' + field.type + '" '+ me.required() +' name="' + field.publicId + '" class="form-control ' + className + ' ' + me.disabled() + '" >' + _value  + '</textarea>' +
        '</div>');

      if (!isDisabled && me.hasDefaultValue() && !value)
        me.setSelectedValue(setValue, getValue);

      me.element.find('textarea').on('keyup', function () {
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };
  };

  var NumericalField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = assetTypeConfiguration.className;

    me.hasValidValue = function() {
      return /^(\d+\.)?\d+$/.test(me.element.find('input').val());
    };

    me.isValid = function(){
      return me.isRequired() && me.hasValue() &&  me.hasValidValue() || (!me.isRequired() && (!me.hasValue() ||  me.hasValue() && me.hasValidValue()));
    };

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : '';

      var unit = _.isUndefined(field.unit) ? '' :  '<span class="input-group-addon ' + className + '">' + field.unit + '</span>';
      var unitClass = _.isUndefined(unit) ? '' : ' unit';

      me.element = $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <input type="text" name="' + field.publicId + '" fieldType = "' + field.type + '" ' + me.required() + ' class="form-control' + unitClass + '" value="' + _value + '"  id="' + className + '" ' + me.disabled() + '>' +
        unit +
        '</div>');

      if (!isDisabled && me.hasDefaultValue() && !value)
        me.setSelectedValue(setValue, getValue);

      me.element.find('input').on('keyup', function () {
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };
  };

  var ReadOnlyFields = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;

    me.editModeRender = function (fieldValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : '-';
      return $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <p class="form-control-readOnly">' + currentLane + '</p>' +
        '</div>'
      );
    };
  };

  var IntegerField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = assetTypeConfiguration.className;

    me.hasValidValue = function() {
      var value = me.element.find('input').val();
      return /^\d+$/.test(value) ? Number(value) === parseInt(value, 10) : false;
    };

    me.isValid = function(){
      return me.isRequired() && me.hasValue() &&  me.hasValidValue() || (!me.isRequired() && (!me.hasValue() ||  me.hasValue() && me.hasValidValue()));
    };

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : '';

      var unit = _.isUndefined(field.unit) ? '' :  '<span class="input-group-addon ' + className + '">' + field.unit + '</span>';
      var unitClass = _.isUndefined(unit) ? '' : ' unit';

      me.element =   $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <input type="text" name="' + field.publicId + '" '+ me.required() +' class="form-control' + unitClass + '"  fieldType = "' + field.type + '" value="' + _value + '"  id="' + className + '" '+ me.disabled() + '>' +
        unit +
        '</div>');


      if (!isDisabled && me.hasDefaultValue()&& !value)
        me.setSelectedValue(setValue, getValue);

      me.element.find('input[type=text]').on('keyup', function () {
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };
  };

  var SingleChoiceField = function (assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = assetTypeConfiguration.className;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var selectedValue = value ? value.value : field.defaultValue ? field.defaultValue : '';

      var template =  _.template(
        '<div class="form-group">' +
        '<label class="control-label">'+ field.label +'</label>' +
        '  <select <%- disabled %> class="form-control <%- className %>" name ="<%- name %>" fieldType ="<%- fieldType %>" <%- required %>><option value="" selected disabled hidden></option><%= optionTags %> </select>' +
        '</div>');


      var optionTags = _.map(field.values, function(value) {
        var selected = value.id.toString() === selectedValue ? " selected" : "";
        return value.hidden ? '' : '<option value="' + value.id + '"' + selected + '>' + value.label + '</option>';
      }).join('');

      me.element = $(template({className: className, optionTags: optionTags, disabled: me.disabled(), name: field.publicId, fieldType: field.type, required: me.required()}));

      me.getValue = function() {
        return me.element.find(":selected").val();
      };
      if (!isDisabled && me.hasDefaultValue() && !value){
        me.setSelectedValue(setValue, getValue);
      }

      me.element.find('select').on('change', function(){
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };

    me.viewModeRender = function (field, currentValue) {
      var value = _.head(currentValue, function(values) { return values.value ; });
      var _value = value ? value.value : '-';

      var someValue = _.find(field.values, function(value) { return value.id.toString() === _value.toString() ; });
      var printValue = someValue ? someValue.label: '-';

      return $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <p class="form-control-static">' + printValue + '</p>' +
        '</div>'
      );
    };
  };

  var MultiSelectField = function (assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var checkedValue = value ? value.value : field.defaultValue ? field.defaultValue : '';

      var template =  _.template(
        '<div class="form-group">' +
        '<label class="control-label">'+ field.label+'</label>' +
        '<div class="choice-group"> ' +
        ' <%= divCheckBox %>' +
        '</div>'+
        '</div>');

      var divCheckBox = _.map(field.values, function(value) {
        var checked =  _.find(checkedValue, function (checkedValue) {return checkedValue === String(value.id);}) ? " checked" : "";
        return '' +
          '<div class = "checkbox">' +
          ' <label>'+ value.label + '<input type = "checkbox" fieldType = "' + field.type + '" '+me.required() +' class="multiChoice-'+sideCode+'"  name = "'+fieldValue.publicId+'" value="'+value.id+'" '+ me.disabled() +' ' + checked + '></label>' +
          '</div>';
      }).join('');

      me.element =  $(template({divCheckBox: divCheckBox}));

      if (!isDisabled && me.hasDefaultValue() && !value)
        me.setSelectedValue(setValue, getValue);

      me.getValue = function() {
        return _.map($('.multiChoice-'+sideCode+':checked'), function(fields) {
          return fields.value; });
      };

      me.element.find('input').on('click', function(){
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };

    me.viewModeRender = function (field, currentValue) {
      var values =  _.map(currentValue, function (values) { return values.value ; });

      var item = _.map(values, function (value) {
        var label = _.find(field.values, function (fieldValue) {return fieldValue.id.toString() === value.toString();}).label;
        return '<li>' + label + '</li>';
      }).join('');

      var template = _.template('<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '<ul class="choice-group">' +
        ' <%= item %>  ' +
        '</ul>' +
        '</div>' );

      return $(template({item: item}));
    };
  };

  var DateField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {

      var someValue = _.head(fieldValue, function(values) { return values.value ; });
      var value = _.isEmpty(someValue) ? (fieldValue.defaultValue ? fieldValue.defaultValue : '') : someValue.value;

      var addDatePickers = function (field, html) {
        var $dateElement = html.find('#' + field.publicId);
        dateutil.addDependentDatePicker($dateElement);
      };

      me.element = $('' +
        '<div class="form-group">' +
        '<label class="control-label">' + field.label + '</label>' +
        '</div>');

      var inputLabel = $('<input type="text" ' + me.disabled() + '/>').addClass('form-control')
        .attr('id', field.publicId)
        .attr('required', me.required())
        .attr('placeholder',"pp.kk.vvvv")
        .attr('fieldType', fieldValue.type)
        .attr('value', value )
        .attr('name', field.publicId).on('keyup datechange', _.debounce(function (target) {
          // tab press
          if (target.keyCode === 9) {
            return;
          }
          me.setSelectedValue(setValue, getValue);
        }, 500));

      if (!isDisabled && me.hasDefaultValue() && !value)
        me.setSelectedValue(setValue, getValue);

      me.element.append(inputLabel);
      addDatePickers(field, me.element);
      return me.element;
    };
  };

  var CheckboxField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var value = _.head(fieldValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : "0";

      var checked = !!parseInt(_value) ? 'checked' : '';

      var checkBoxElement = "input[name = '" + field.publicId + '-' + sideCode + "']";

      me.element = $('' +
        '<div class="form-group">' +
        '<label class="control-label">'+ field.label+'</label>' +
        '<div class="choice-group"> ' +
        '<input type = "checkbox" fieldType = "' + field.type + '" '+ me.required() +' class="multiChoice-' + sideCode + '" name = "' + field.publicId + '-' + sideCode + '" value=' + _value +' '+ me.disabled() +' '+  checked +'>' +
        '</div>'+
        '</div>');

      me.getValue = function() {
        return $(checkBoxElement).prop('checked') ? 1: 0;
      };

      me.hasValue = function(){
        return true;
      };

      me.setValue = function(value) {
        $(checkBoxElement).prop('checked', !!(value));
      };

      if(!isDisabled && (me.hasDefaultValue() || me.isRequired()) && !value) {
        me.setValue(me.getValue());
        me.setSelectedValue(setValue, getValue);
      }

      me.element.find('input').on('click', function(){
        me.setValue(me.getValue());
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };

    me.viewModeRender = function (field, currentValue) {
      var curr = _.isEmpty(currentValue) ? '0' : currentValue[0].value;

      var template = _.template('<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <p class="form-control-static">' +
        ' <%= divCheckBox %>  ' +
        '</p>' +
        '</div>' );

      var someValue = _.find(field.values, function(value) {return String(value.id) === curr; });
      var value = someValue ? someValue.label : '-';
      return $(template({divCheckBox : value}));
    };
  };

  var TimePeriodField = function(assetTypeConfiguration, field, isDisabled){
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = assetTypeConfiguration.className;
    var dayLabels = {1: "Su", 2: "Ma–Pe", 7: "La"};

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {

      var existingValidityPeriodElements =
        _(_.map(fieldValue, function(values) { return values.value ; }))
          .sortBy('days', 'startHour', 'startMinute', 'endHour', 'endMinute')
          .map(validityPeriodElement)
          .join('');

      function newValidityPeriodElement() {
        return '' +
          '<li><div class="form-group new-validity-period">' +
          '  <select class="form-control select" ' + me.disabled() + '>' +
          '    <option class="empty" disabled selected>Lisää voimassaoloaika</option>' +
          '    <option value="1">Su</option>' +
          '    <option value="2">Ma–Pe</option>' +
          '    <option value="7">La</option>' +
          '  </select>' +
          '</div></li>';
      }

      function validityPeriodElement(period) {
        return '' +
          '<li><div class="form-group existing-validity-period" data-days="' + period.days + '">' +
          '  <button class="delete btn-delete"' + me.disabled() + '>x</button>' +
          '  <label class="control-label daysf">' +
          dayLabels[period.days] +
          '  </label>' +
          hourElement(period.startHour, 'start') +
          '  <span class="minute-separator"></span>' +
          minutesElement(period.startMinute, 'start') +
          '  <span class="hour-separator"> - </span>' +
          hourElement(period.endHour, 'end') +
          '  <span class="minute-separator"></span>' +
          minutesElement(period.endMinute, 'end') +
          '</div></li>';
      }

      function hourElement(selectedHour, type) {
        var className = type + '-hour';
        return '' +
          '<select class="form-control sub-control select ' + className + '"' + me.disabled()+ '>' +
          hourOptions(selectedHour, type) +
          '</select>';
      }

      function minutesElement(selectedMinute, type) {
        var className = type + '-minute';
        return '' +
          '<select class="form-control sub-control select ' + className + '"' + me.disabled()+ '>' +
          minutesOptions(selectedMinute) +
          '</select>';
      }

      function hourOptions(selectedOption, type) {
        var range = type === 'start' ? _.range(0, 24) : _.range(1, 25);
        return _.map(range, function (hour) {
          var selected = hour === selectedOption ? 'selected' : '';
          return '<option value="' + hour + '" ' + selected + '>' + hour + '</option>';
        }).join('');
      }

      function minutesOptions(selectedOption) {
        var range = _.range(0, 60, 5);
        return _.map(range, function (minute) {
          var selected = minute === selectedOption ? 'selected' : '';
          return '<option value="' + minute + '" ' + selected + '>' + (minute<10 ? '0' + minute : minute) + '</option>';
        }).join('');
      }

      var template = _.template('' +
        '<div class="validity-period-group">' +
        '<label>Voimassaoloaika (lisäkilvessä):</label>' +
        ' <ul>' +
        '   <%= existingValidityPeriodElements %>' +
        newValidityPeriodElement() +
        ' </ul>');

      me.element = $(template({existingValidityPeriodElements: existingValidityPeriodElements}));

      me.getPropertyValue = function(){
        var values = me.getValue();
        return me.createPropertyValue(values);
      };

      me.compare = function(propertyValueA, propertyValueB){
        var isEqual = function(valueA , valueB) {
          return valueA.startHour === valueB.startHour && valueA.startMinute === valueB.startMinute && valueA.endHour === valueB.endHour && valueA.endMinute === valueB.endMinute && valueA.days === valueB.days;
        };

        var isRemoved = function(firstProperty, secondProperty) {
          _.remove(firstProperty.values, function (valuesA) {
            return !_.isUndefined(_.find(secondProperty.values, function (valuesB) {
              return isEqual(valuesB.value, valuesA.value);
            }));
          });
          return _.isEmpty(firstProperty.values);
        };
        return isRemoved(_.cloneDeep(propertyValueA), _.cloneDeep(propertyValueB)) && isRemoved(_.cloneDeep(propertyValueB), _.cloneDeep(propertyValueA));
      };

      me.getValue = function() {
        var periodElements = me.element.find('.existing-validity-period');
        return _.map(periodElements, function (element) {
          return { value: {
              startHour: parseInt($(element).find('.start-hour').val(), 10),
              startMinute: parseInt($(element).find('.start-minute').val(), 10),
              endHour: parseInt($(element).find('.end-hour').val(), 10),
              endMinute: parseInt($(element).find('.end-minute').val(), 10),
              days: parseInt($(element).data('days'), 10)
            }};
        });
      };

      me.element.on('click', '.existing-validity-period .delete', function(event) {
        $(event.target).parent().parent().remove();
        me.setSelectedValue(setValue, getValue);
      });

      me.element.on('change', '.existing-validity-period .select', function(event) {
        me.setSelectedValue(setValue, getValue);
      });

      me.element.on('change', '.new-validity-period select', function(event) {
        $(event.target).closest('.validity-period-group ul').append(newValidityPeriodElement());
        $(event.target).parent().parent().replaceWith(validityPeriodElement({
          days: $(event.target).val(),
          startHour: 0,
          startMinute: 0,
          endHour: 24,
          endMinute: 0
        }));
        me.setSelectedValue(setValue, getValue);
      });

      return me.element;
    };

    me.viewModeRender = function (field, currentValue) {
      var validityPeriodLabel = _.isEmpty(currentValue) ? '' : '<label>Voimassaoloaika (lisäkilvessä):</label>';

      var validityPeriodTable = _.map(currentValue, function(value) {
        var period = value.value;
        return '' +
          '<li>' + dayLabels[period.days] + " " + period.startHour + ":" + ("0" + period.startMinute).slice(-2)  + " - " + period.endHour + ":" + ("0" + period.endMinute).slice(-2) + '</li>';
      }).join('');

      return $('' +
        '<div class="form-group read-only">' +
        '<ul class="form-control-static validity-period-group">' +
        validityPeriodLabel +
        validityPeriodTable +
        '</ul>' +
        '</div>' );
    };
  };

  var DatePeriodField = function(assetTypeConfiguration, field, isDisabled) {
    DynamicField.call(this, field, isDisabled);
    var me = this;
    var className = field.publicId;
    var elementNumber = 0;

    me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
      var buttons = '<div class="form-group date-time-period-buttons">' +
        '<button class="form-control btn edit-only editable btn-secondary add-period"' + me.disabled() +' >Lisää kausi</button>' +
        '<span></span>' +
        '<button class="form-control btn edit-only btn-secondary remove-period"' + me.disabled() +'>Poista kausi</button>'+
        '</div>';

      var handleButton = function() {
        var $element = me.element;
        var removeAllowed = me.element.find('.existing-date-period').length > 1;
        $element.find('.add-period').showElement(!removeAllowed);
        $element.find('.remove-period').showElement(removeAllowed);
      };

      me.getPropertyValue = function(){
        var values = me.getValue();
        return me.createPropertyValue(values);
      };

      me.getValue = function() {
        var periodElements = me.element.find('.existing-date-period');
        return _.map(periodElements, function (element) {
          return { value: {
              startDate: $(element).find('.'+className+'-start').val(),
              endDate: $(element).find('.'+className+'-end').val()
            }};
        });
      };

      me.hasValue = function() {
        return _.some(me.getValue(), function (values) {
          var period = values.value;
          return !_.isEmpty(period.startDate) && !_.isEmpty(period.endDate);
        });
      };

      me.isValid = function(){
        //both Dates empties or filled
        var bothDates =_.every(me.getValue(), function (values) {
          var period = values.value;
          return !(_.isEmpty(period.startDate) ^ _.isEmpty(period.endDate));
        });
        return bothDates && (!me.isRequired() || me.isRequired() && me.hasValue());
      };

      var addDatePickers = function (elementNumber) {
        var $startDate = me.element.find('#datePeriod-start' + elementNumber);
        var $endDate = me.element.find('#datePeriod-end' + elementNumber);

        dateutil.addTwoDependentDatePickers($startDate, $endDate);
      };

      var inputLabel = function(type, value) {
        return $('<input type="text" ' + me.disabled() + '/>').addClass( className + ' form-control ' + className+'-'+type)
          .attr('id', 'datePeriod-'+type+elementNumber)
          .attr('required', me.required())
          .attr('placeholder',"pp.kk.vvvv")
          .attr('fieldType', fieldValue.type)
          .attr('value', value )
          .attr('autocomplete',"off")
          .attr('name', field.publicId).on('keyup datechange', _.debounce(function (target) {
            // tab press
            if (target.keyCode === 9) {
              return;
            }
            me.setValue(me.getValue());
            me.setSelectedValue(setValue, getValue);
          }, 500));
      };

      if (!isDisabled && me.hasDefaultValue() && !value) {
        me.setValue(me.getValue());
        me.setSelectedValue(setValue, getValue);
      }

      var existingDatePeriodElements =
        _(_.map(fieldValue, function(values) { return values.value ; }))
          .sortBy('startDate', 'endDate')
          .map(datePeriodElement)
          .join('');

      function datePeriodElement(periods) {
        return createPeriodElement(periods)[0].outerHTML;
      }

      function createPeriodElement(period) {
        elementNumber = elementNumber + 1;
        return $('' +
          '<li class="form-group existing-date-period">')
          .append(inputLabel('start', period ? period.startDate : undefined))
          .append('<span class="date-separator"> - </span>')
          .append(inputLabel('end', period ? period.endDate : undefined))
          .append(me.disabled() ? '' : fieldValue.multiElement ? buttons : '');
      }

      var template = _.template('' +
        '<div class="form-group date-time-period-group">' +
        '<label class="control-label">' + field.label + '</label>' +
        ' <ul >' +
        '   <%= existingDatePeriodElements %>' +
        (_.isEmpty(existingDatePeriodElements) ? createPeriodElement()[0].outerHTML : '') +
        ' </ul>'+
        '</div>');


      me.element = $(template({existingDatePeriodElements: existingDatePeriodElements}));
      for (var elementNumb = 1; elementNumb <= elementNumber; elementNumb++) {
        addDatePickers(elementNumb);
      }

      me.element.on('click', '.remove-period', function(event) {
        $(event.target).parent().parent().remove();
        me.setSelectedValue(setValue, getValue);

        handleButton();
      });

      me.element.on('datechange', function() {
        me.setSelectedValue(setValue, getValue);
      });

      me.element.on('click', '.add-period', function() {
        $(event.target).closest('.date-time-period-group ul').append(createPeriodElement()[0].outerHTML);
        addDatePickers(elementNumber);
        handleButton();
      });

      handleButton();
      return me.element;
    };

    me.isValid = function(){
      return !me.isRequired() || me.isRequired() && me.hasValue();
    };

    me.compare = function(propertyValueA, propertyValueB){
      var isEqual = function(valueA , valueB) {
        return valueA.startDate === valueB.startDate && valueA.endDate === valueB.endDate;
      };

      var isRemoved = function(firstProperty, secondProperty) {
        _.remove(firstProperty.values, function (valuesA) {
          return !_.isUndefined(_.find(secondProperty.values, function (valuesB) {
            return isEqual(valuesB.value, valuesA.value);
          }));
        });
        return _.isEmpty(firstProperty.values);
      };
      return isRemoved(_.cloneDeep(propertyValueA), _.cloneDeep(propertyValueB)) && isRemoved(_.cloneDeep(propertyValueB), _.cloneDeep(propertyValueA));
    };

    me.viewModeRender = function (field, currentValue) {
      var datePeriodTable = _.map(currentValue, function(values) {
        return _.map(values, function(period){ return '' +
          '<li>' + period.startDate + " - " + period.endDate + '</li>';}).join('');
      }).join('');

      return $('' +
        '<div class="form-group read-only">' +
        '<label class="control-label">Kelirikkokausi</label>' +
        '<ul class="form-control-static date-period-group">' +
        datePeriodTable +
        '</ul>' +
        '</div>' );
    };

  };

  //hides field when in edit mode, show in view mode
  var HiddenReadOnlyFields = function(assetTypeConfiguration){
    DynamicField.call(this, assetTypeConfiguration);
    var me = this;

    me.viewModeRender = function (field, currentValue) {
      var value = _.head(currentValue, function(values) { return values.value ; });
      var _value = value ? value.value : field.defaultValue ? field.defaultValue : '-';

      var someValue = _.find(field.values, function(value) { return value.id.toString() === _value.toString() ; });
      var printValue = _.isUndefined(someValue) ? _value : someValue.label;

      return $('' +
        '<div class="form-group">' +
        '   <label class="control-label">' + field.label + '</label>' +
        '   <p class="form-control-static">' + printValue + '</p>' +
        '</div>'
      );
    };
  };

  root.laneModellingFormFields = [
    {name: 'long_text', fieldType: TextualLongField},
    {name: 'single_choice', fieldType: SingleChoiceField},
    {name: 'date', fieldType: DateField},
    {name: 'multiple_choice', fieldType: MultiSelectField},
    {name: 'integer', fieldType: IntegerField},
    {name: 'number', fieldType: NumericalField},
    {name: 'text', fieldType: TextualField},
    {name: 'checkbox', fieldType: CheckboxField},
    {name: 'read_only_number', fieldType: ReadOnlyFields},
    {name: 'read_only_text', fieldType: ReadOnlyFields},
    {name: 'time_period', fieldType: TimePeriodField},
    {name: 'date_period', fieldType: DatePeriodField},
    {name: 'hidden_read_only_number', fieldType: HiddenReadOnlyFields}
  ];

  var currentLane = 11;

  var mainLaneFormStructure = {
    fields : [
      {
        label: 'Kaista', type: 'read_only_number', required: 'required', publicId: "lane_code", defaultValue: 11, weight: 1
      },
      {
        label: 'Kaistan tyypi', required: 'required', type: 'single_choice', publicId: "lane_type", defaultValue: "1", weight: 2,
        values: [
          {id: 1, label: 'Pääkaista'}
          ]
      },
      {
        label: 'Kaista jatkuvuus', required: 'required', type: 'single_choice', publicId: "lane_continuity", defaultValue: "1", weight: 3,
        values: [
          {id: 1, label: 'Jatkuva'},
          {id: 2, label: 'Jatkuu toisella kaistanumerolla'},
          {id: 3, label: 'Kääntyvä'},
          {id: 4, label: 'Päättyvä'},
          {id: 5, label: 'Jatkuva, osoitettu myös oikealle kääntyville'},
          {id: 6, label: 'Jatkuva, osoitettu myös vasemmalle kääntyville'},
        ]
      },
      {
        label: 'Kaista ominaisuustieto', type: 'text', publicId: "lane_information", weight: 4
      }
    ]
  };

  var currentFormStructure;

  var defaultFormStructure;

  root.LaneModellingToolForm = function (formStructure) {
    defaultFormStructure = formStructure;

    if(currentLane == "11"){
      currentFormStructure = mainLaneFormStructure;
    }else{
      defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
      currentFormStructure = defaultFormStructure;
    }

    var me = this;
    var _assetTypeConfiguration;
    var AvailableForms = function(){
      var formFields = {};

      this.addField = function(field, sideCode){
        if(!formFields['sidecode_'+sideCode])
          formFields['sidecode_'+sideCode] = [];
        formFields['sidecode_'+sideCode].push(field);
      };

      this.getFields = function(sideCode){
        if(!formFields['sidecode_'+sideCode])
          Error("The form of the sidecode " + sideCode + " doesn't exist");
        return formFields['sidecode_'+sideCode];
      };

      this.getAllFields = function(){
        return _.flatten(_.map(formFields, function(form) {
          return form;
        }));
      };

      this.removeFields = function(sideCode){
        if(!formFields['sidecode_'+sideCode])
          throw Error("The form of the sidecode " + sideCode + " doesn't exist");

        formFields['sidecode_'+sideCode] = [];
      };
    };

    var forms = new AvailableForms();

    me.initialize = function(assetTypeConfiguration, feedbackModel){
      var rootElement = $('#feature-attributes');
      _assetTypeConfiguration = assetTypeConfiguration;
      new FeedbackDataTool(feedbackModel, assetTypeConfiguration.layerName, assetTypeConfiguration.authorizationPolicy, assetTypeConfiguration.singleElementEventCategory);

      var updateStatusForMassButton = function(element) {
        if(assetTypeConfiguration.selectedLinearAsset.isSplitOrSeparated()) {
          element.prop('disabled', !(me.isSaveable() && me.isSplitOrSeparatedAllowed()));
        } else
          element.prop('disabled', !(me.isSaveable()));
      };

      eventbus.on(events('selected', 'cancelled'), function () {
        var isDisabled = false;
        // rootElement.find('#feature-attributes-header').html(me.renderHeader(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
        rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-footer').html(me.renderFooter(_assetTypeConfiguration.selectedLinearAsset));
      });

      eventbus.on(events('unselect'), function() {
        // rootElement.find('#feature-attributes-header').empty();
        rootElement.find('#feature-attributes-form').empty();
        rootElement.find('#feature-attributes-footer').empty();
      });

      eventbus.on('layer:selected', function(layer) {
        if(_assetTypeConfiguration.layerName === layer){
          $('ul[class=information-content]').empty();

          if(_assetTypeConfiguration.isVerifiable)
            renderLinkToWorkList(layer);
          if(_assetTypeConfiguration.hasInaccurate)
            renderInaccurateWorkList(layer);
        }
      });

      eventbus.on('application:readOnly', function(){
        if(_assetTypeConfiguration.layerName ===  applicationModel.getSelectedLayer() && _assetTypeConfiguration.selectedLinearAsset.count() !== 0) {
          var isDisabled = false;
          // rootElement.find('#feature-attributes-header').html(me.renderHeader(_assetTypeConfiguration.selectedLinearAsset));
          rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
          rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
          rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
          rootElement.find('#feature-attributes-footer').html(me.renderFooter(_assetTypeConfiguration.selectedLinearAsset));
        }
      });

      eventbus.on("massDialog:rendered", function(buttonElement){
        eventbus.on(multiEvents('valueChanged'), function() {
          updateStatusForMassButton(buttonElement);
        });
      });

      function events() {
        return _.map(arguments, function(argument) { return _assetTypeConfiguration.singleElementEventCategory + ':' + argument; }).join(' ');
      }

      function multiEvents() {
        return _.map(arguments, function(argument) { return _assetTypeConfiguration.multiElementEventCategory + ':' + argument; }).join(' ');
      }
    };

    me.renderAvailableFormElements = function(asset, isReadOnly, sideCode, setAsset, getValue, isDisabled, alreadyRendered) {
      if(alreadyRendered)
        forms.removeFields(sideCode);
      var fieldGroupElement = $('<div class = "input-unit-combination lane-' + currentLane + '" >');
      _.each(_.sortBy(currentFormStructure.fields, function(field){ return field.weight; }), function (field) {
        var fieldValues = [];
        if (asset.properties) {
          var existingProperty = _.find(asset.properties, function (property) { return property.publicId === field.publicId; });
          if(!_.isUndefined(existingProperty))
            fieldValues = existingProperty.values;
        }
        var dynamicField = _.find(laneModellingFormFields, function (availableFieldType) { return availableFieldType.name === field.type; });
        var fieldType = new dynamicField.fieldType(_assetTypeConfiguration, field, isDisabled);
        forms.addField(fieldType, sideCode);
        var fieldElement = isReadOnly ? fieldType.viewModeRender(field, fieldValues) : fieldType.editModeRender(fieldValues, sideCode, setAsset, getValue);

        fieldGroupElement.append(fieldElement);

      });
      return fieldGroupElement;
    };

    function _isReadOnly(selectedAsset){
      return applicationModel.isReadOnly() || !checkAuthorizationPolicy(selectedAsset);
    }

    var createHeaderElement = function(selectedAsset) {
      var title = function () {
        if(selectedAsset.isUnknown(currentLane) || selectedAsset.isSplit(currentLane)) {
          return '<span class="read-only-title" style="display: block">' +_assetTypeConfiguration.title + '</span>' +
            '<span class="edit-mode-title" style="display: block">' + _assetTypeConfiguration.newTitle + '</span>';
        }
        return selectedAsset.count() === 1 ?
          '<span>Kohteen ID: ' + selectedAsset.getId() + '</span>' : '<span>' + _assetTypeConfiguration.title + '</span>';
      };

      return $(title());
    };

    var createPreviewHeaderElement = function(laneNumbers) {
      var createNumber = function (number) {
        var previewButton = $('<li class="preview lane ' + number + '" ' + 'style="display: inline-block; width: 8%;height:10%;background-color:dimgrey;">' + number + '</li>').click(function() {
          $(".preview .lane").css('border', '1px solid white');
          $(this).css('border', '1px solid yellow');
          currentLane = parseInt(number);
          var isDisabled = false;
          var rootElement = $('#feature-attributes');

          if(currentLane == "11"){
            currentFormStructure = mainLaneFormStructure;
          }else{
            defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
            currentFormStructure = defaultFormStructure;
          }

          rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
          rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
          rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
        });

        if(number == currentLane){
          return previewButton.css("border", "1px solid yellow");
        }else{
          return previewButton.css("border", "1px solid white");
        }
      };

      var numbers = _.sortBy(laneNumbers);

      var odd = _.filter(numbers, function (number) {
        return number % 2 !== 0;
      });
      var even = _.filter(numbers, function (number) {
        return number % 2 === 0;
      });

      var preview = function () {
        var previewList = $('<ul id="preview" class="preview" style="margin-top: 1%; display: inline; color: white; margin:auto;"></ul>');

        var oddListElements = _.map(odd, function (number) {
              return createNumber(number);
            });

        var evenListElements = _.map(_.reverse(even), function (number) {
          return createNumber(number);
        });

        var previewDiv = $('<div style="text-align:center;"></div>').append(previewList.append(evenListElements).append(oddListElements).append('<hr style="width: 90%;">'));

        return previewDiv;
      };

      return preview();
    };

    var createLaneButtons = function(selectedAsset){
      var laneNumbers = _.map(selectedAsset.get(), function (lane){
        return _.find(lane.properties, function (property) {
          return property.publicId == "lane_code";
        }).values[0].value;
      });

      var odd =_.filter(laneNumbers, function (number) {
        return number % 2 !== 0;
      });

      var even = _.filter(laneNumbers, function (number) {
        return number % 2 === 0;
      });

      var addLeftLane = $('<li>').append($('<button class="btn btn-secondary add-lane-left">Lisää kaista oikealle puolelle</button>').click(function() {
        $(".preview .lane").css('border', '1px solid white');

        var nextLaneNumber;
        if(_.isEmpty(even)){
          nextLaneNumber = parseInt(odd.toString()[0] + '2');
        }else{
          nextLaneNumber = parseInt(_.max(even)) + 2;
        }

        currentLane = nextLaneNumber;
        selectedAsset.setNewLane(nextLaneNumber);

        var isDisabled = false;
        var rootElement = $('#feature-attributes');

        defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
        currentFormStructure = defaultFormStructure;

        rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
        rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
      }));

      var addRightLane = $('<li>').append($('<button class="btn btn-secondary add-lane-right">Lisää kaista vasemmalle puolelle</button>').click(function() {
        $(".preview .lane").css('border', '1px solid white');

        var nextLaneNumber = parseInt(_.max(odd)) + 2;

        currentLane = nextLaneNumber;
        selectedAsset.setNewLane(nextLaneNumber);

        var isDisabled = false;
        var rootElement = $('#feature-attributes');

        defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
        currentFormStructure = defaultFormStructure;

        rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
        rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
      }));

      if(_.max(even) == 18 || _.max(even) == 28)
        addLeftLane.find('.add-lane-left').prop("disabled", true);

      if(_.max(odd) == 19 || _.max(odd) == 29)
        addRightLane.find('.add-lane-right').prop("disabled", true);

      return $('<ul style="list-style: none;">').append('' + '<button disabled class="btn btn-secondary">Lisää kaista tieosoitteen avulla</button>').append(addLeftLane).append(addRightLane);
    };

      me.editModeRender = function (fieldValue, sideCode, setValue, getValue) {
        var value = _.head(fieldValue, function(values) { return values.value ; });
        var _value = value ? value.value : field.defaultValue ? field.defaultValue : '';

        me.element = $('' +
          '<div class="form-group">' +
          '   <label class="control-label">' + field.label + '</label>' +
          '   <input type="text" fieldType = "' + field.type + '" '+ me.required() +' name="' + field.publicId + '" class="form-control ' + className + '" ' + me.disabled()  + ' value="' + _value + '" >' +
          '</div>');

        if (!isDisabled && me.hasDefaultValue() && !value)
          me.setSelectedValue(setValue, getValue);

        me.element.find('input[type=text]').on('keyup', function(){
          me.setSelectedValue(setValue, getValue);
        });
        return me.element;
      };


    var createFooterElement = function() {
      return $('<div class="linear-asset form-controls" style="display: none"></div>')
        .append(new VerificationButton(_assetTypeConfiguration).element)
        .append(new SaveButton(_assetTypeConfiguration, currentFormStructure).element)
        .append(new CancelButton(_assetTypeConfiguration).element);
    };

    me.renderHeader = function(selectedAsset) {
      var isReadOnly = _isReadOnly(selectedAsset);

      var header = createHeaderElement(selectedAsset);

      header.filter('.read-only-title').toggle(isReadOnly);
      header.filter('.edit-mode-title').toggle(!isReadOnly);
      header.filter('.form-controls').toggle(!isReadOnly);

      return header;
    };

    me.renderPreview = function(selectedAsset) {
      var previewHeader = createPreviewHeaderElement(_.map(selectedAsset.get(), function (lane){
        return _.find(lane.properties, function (property) {
          return property.publicId == "lane_code";
        }).values[0].value;
      }));

      return previewHeader;
    };

    me.renderLaneButtons = function(selectedAsset) {
      var isReadOnly = _isReadOnly(selectedAsset);
      var laneButtons = createLaneButtons(selectedAsset);
      //Hide or show elements depending on the readonly mode
      laneButtons.toggle(!isReadOnly);

      return laneButtons;
    };

    me.renderFooter = function(selectedAsset) {
      var isReadOnly = _isReadOnly(selectedAsset);
      var footer = createFooterElement();
      //Hide or show elements depending on the readonly mode
      footer.filter('.form-controls').toggle(!isReadOnly);

      return footer;
    };

    me.renderForm = function (selectedAsset, isDisabled, isMassUpdate) {
      forms = new AvailableForms();
      var isReadOnly = _isReadOnly(selectedAsset);
      var asset = _.find(selectedAsset.get(), function (lane){
        return _.find(lane.properties, function (property) {
          return property.publicId == "lane_code" && property.values[0].value == currentLane;
        });
      });

      var body = createBodyElement(selectedAsset);

      renderFormElements(asset, isReadOnly, '', selectedAsset.setValue, selectedAsset.getValue, selectedAsset.removeValue, isDisabled, body);

      if(!isReadOnly)
        renderExpireAndDeleteButtonsElement(selectedAsset, body);

      //Hide or show elements depending on the readonly mode
      toggleBodyElements(body, isReadOnly);
      return body;
    };

    // function renderSeparateButtonElement(selectedAsset, body, isMassUpdate){
    //   if(selectedAsset.isSeparable() && !_isReadOnly(selectedAsset) && !isMassUpdate){
    //     var separateElement = $(''+
    //       '<div class="form-group editable">' +
    //       '  <label class="control-label"></label>' +
    //       '  <button class="cancel btn btn-secondary" id="separate-limit">Jaa kaksisuuntaiseksi</button>' +
    //       '</div>');
    //
    //     separateElement.find('#separate-limit').on('click', function() { _assetTypeConfiguration.selectedLinearAsset.separate(); });
    //
    //     body.find('.form').append(separateElement);
    //   }
    // }

    function renderExpireAndDeleteButtonsElement(selectedAsset, body){
      var deleteLane = $('<li>').append($('<button class="btn btn-secondary delete-lane">Poista kaista</button>').click(function() {
        $(".preview .lane").css('border', '1px solid white');
        selectedAsset.removeLane(currentLane);

        var asset = _.find(selectedAsset.get(), function (lane){
          return _.find(lane.properties, function (property) {
            return property.publicId == "lane_code" && property.values[0].value == currentLane;
          });
        });

        if(_.isUndefined(asset)){
          if(currentLane.toString()[1] == "2"){
            currentLane = parseInt(currentLane.toString()[0] + '1');
          }else{
            currentLane = currentLane-2;
          }
        }

        var isDisabled = false;
        var rootElement = $('#feature-attributes');

        if(currentLane.toString()[1] == "1"){
          currentFormStructure = mainLaneFormStructure;
        }else{
          defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
          currentFormStructure = defaultFormStructure;
        }

        rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
        rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
      }));

      var expireLane = $('<li>').append($('<button disabled class="btn btn-secondary delete-lane">Paata kaista</button>').click(function() {
        $(".preview .lane").css('border', '1px solid white');
        selectedAsset.removeLane(currentLane);

        var asset = _.find(selectedAsset.get(), function (lane){
          return _.find(lane.properties, function (property) {
            return property.publicId == "lane_code" && property.values[0].value == currentLane;
          });
        });

        if(_.isUndefined(asset)){
          if(currentLane.toString()[1] == "2"){
            currentLane = parseInt(currentLane.toString()[0] + '1');
          }else{
            currentLane = currentLane-2;
          }
        }

        var isDisabled = false;
        var rootElement = $('#feature-attributes');

        if(currentLane.toString()[1] == "1"){
          currentFormStructure = mainLaneFormStructure;
        }else{
          defaultFormStructure.fields[0] = {label: 'Kaista', type: 'read_only_number', publicId: "lane_number", defaultValue: currentLane, weight: 1};
          currentFormStructure = defaultFormStructure;
        }

        rootElement.find('#feature-attributes-form').html(me.renderForm(_assetTypeConfiguration.selectedLinearAsset, isDisabled));
        rootElement.find('#feature-attributes-form').prepend(me.renderPreview(_assetTypeConfiguration.selectedLinearAsset));
        rootElement.find('#feature-attributes-form').append(me.renderLaneButtons(_assetTypeConfiguration.selectedLinearAsset));
      }));

      if(currentLane.toString()[1] !== "1")
        body.find('.form').append($('<div>')).append($('<ul style="list-style: none;">').append(deleteLane).append(expireLane));
    }

    function renderFormElements(asset, isReadOnly, sideCode, setValueFn, getValueFn, removeValueFn, isDisabled, body) {
      var sideCodeClass = generateClassName(sideCode);

      var formGroup = $('' + '<div class="dynamic-form editable form-editable-'+ sideCodeClass +'">' + '</div>');

      setValueFn(currentLane, {properties: asset.properties});

      body.find('.form').append(formGroup);
      body.find('.form-editable-' + sideCodeClass).append(me.renderAvailableFormElements(asset, isReadOnly, sideCode, setValueFn, getValueFn, isDisabled));

      return body;
    }

    function renderLinkToWorkList(layerName) {
      $('ul[class=information-content]').append('' +
        '<li><button id="unchecked-links" class="unchecked-linear-assets btn btn-tertiary" onclick=location.href="#work-list/' + layerName + '">Vanhentuneiden kohteiden lista</button></li>');
    }

    function renderInaccurateWorkList(layerName) {
      $('ul[class=information-content]').append('<li><button id="work-list-link-errors" class="wrong-linear-assets btn btn-tertiary" onclick=location.href="#work-list/' + layerName + 'Errors">Laatuvirhelista</button></li>');
    }

    function createSideCodeMarker(sideCode) {
      if (_.isUndefined(sideCode) || sideCode === '')
        return '';

      return '<span class="marker">' + sideCode + '</span>';
    }

    var userInformationLog = function() {
      var selectedAsset = _assetTypeConfiguration.selectedLinearAsset;
      var authorizationPolicy = _assetTypeConfiguration.authorizationPolicy;

      var hasMunicipality = function(linearAsset) {
        return _.some(linearAsset.get(), function(asset){
          return authorizationPolicy.hasRightsInMunicipality(asset.municipalityCode);
        });
      };

      var limitedRights = 'Käyttöoikeudet eivät riitä kohteen muokkaamiseen. Voit muokata kohteita vain oman kuntasi alueelta.';
      var noRights = 'Käyttöoikeudet eivät riitä kohteen muokkaamiseen.';
      var message = '';

      if(!authorizationPolicy.isOperator() && (authorizationPolicy.isMunicipalityMaintainer() || authorizationPolicy.isElyMaintainer()) && !hasMunicipality(selectedAsset)) {
        message = limitedRights;
      } else if(!checkAuthorizationPolicy(selectedAsset))
        message = noRights;

      if(message) {
        return '' +
          '<div class="form-group user-information">' +
          '<p class="form-control-static user-log-info">' + message + '</p>' +
          '</div>';
      } else
        return '';
    };

    var informationLog = function (date, username) {
      return date ? (date + ' / ' + username) : '-';
    };

    function createBodyElement(selectedAsset) {
      var info = {
        // modifiedBy :  selectedAsset.getModifiedBy() || '',
        // modifiedDate : selectedAsset.getModifiedDateTime() ? ' ' + selectedAsset.getModifiedDateTime(): '',
        // createdBy : selectedAsset.getCreatedBy() || '',
        // createdDate : selectedAsset.getCreatedDateTime() ? ' ' + selectedAsset.getCreatedDateTime(): '',
        // verifiedBy : selectedAsset.getVerifiedBy(),
        // verifiedDateTime : selectedAsset.getVerifiedDateTime()
      };

      var verifiedFields = function() {
        return (_assetTypeConfiguration.isVerifiable && info.verifiedBy && info.verifiedDateTime) ?
          '<div class="form-group">' +
          '   <p class="form-control-static asset-log-info">Tarkistettu: ' + informationLog(info.verifiedDateTime, info.verifiedBy) + '</p>' +
          '</div>' : '';
      };

      return $('<div class="wrapper read-only">' +
        '   <div class="form form-horizontal form-dark asset-factory">' +
        '     <div class="form-group">' +
        '       <p class="form-control-static asset-log-info">Lis&auml;tty j&auml;rjestelm&auml;&auml;n: ' + informationLog(info.createdDate, info.createdBy)+ '</p>' +
        '     </div>' +
        '     <div class="form-group">' +
        '       <p class="form-control-static asset-log-info">Muokattu viimeksi: ' + informationLog(info.modifiedDate, info.modifiedBy) + '</p>' +
        '     </div>' +
        verifiedFields() +
        '     <div class="form-group">' +
        '       <p class="form-control-static asset-log-info">Linkkien lukumäärä: ' + selectedAsset.count() + '</p>' +
        '     </div>' +
        userInformationLog() +
        '   </div>' +
        '</div>');
    }

    function toggleBodyElements(rootElement, isReadOnly) {
      rootElement.find('.editable .form-control-static').toggle(isReadOnly);
      rootElement.find('.editable .edit-control-group').toggle(!isReadOnly);
    }

    function generateClassName(sideCode) {
      return sideCode ? _assetTypeConfiguration.className + '-' + sideCode : _assetTypeConfiguration.className;
    }

    function checkAuthorizationPolicy(selectedAsset){
      var auth = _assetTypeConfiguration.authorizationPolicy || function() { return false; };
      return auth.validateMultiple(selectedAsset.get());
    }

    me.isSplitOrSeparatedAllowed = function(){
      //When both are deleted
      if(_.isEmpty(forms.getAllFields()))
        return false;

      if(_.isEmpty(forms.getFields('a')) || _.isEmpty(forms.getFields('b')))
        return true;

      return _.some(forms.getFields('a'), function(fieldA){
        var propertyValueA = fieldA.getPropertyValue();
        var fieldB = _.head(_.filter(forms.getFields('b'), function (fieldB) {return propertyValueA.publicId === fieldB.getPropertyValue().publicId;}));

        return !fieldA.compare(propertyValueA, fieldB.getPropertyValue()) || !_.isEqual(fieldB.disabled(), fieldA.disabled());
      });
    };

    me.isSaveable = function(field){
      var otherSaveCondition = function () {
        if(_assetTypeConfiguration.saveCondition)
          return _assetTypeConfiguration.saveCondition(_assetTypeConfiguration.selectedLinearAsset.get());
        return true;
      };
      return _.every(forms.getAllFields(), function(field){
        return field.isValid();
      })&& otherSaveCondition();
    };

    function events() {
      return _.map(arguments, function(argument) { return _assetTypeConfiguration.singleElementEventCategory + ':' + argument; }).join(' ');
    }

    var SaveButton = function(assetTypeConfiguration) {

      var element = $('<button />').addClass('save btn btn-primary').prop('disabled', !assetTypeConfiguration.selectedLinearAsset.isDirty()).text('Tallenna').on('click', function() {
        currentLane = parseInt(currentLane.toString()[0] + '1');
        assetTypeConfiguration.selectedLinearAsset.save();
      });

      var updateStatus = function(element) {
        if(assetTypeConfiguration.selectedLinearAsset.isSplitOrSeparated()) {
          element.prop('disabled', !(me.isSaveable(forms.getFields('a')) && me.isSaveable(forms.getFields('b')) && me.isSplitOrSeparatedAllowed()));
        } else
          element.prop('disabled', !(me.isSaveable(forms.getAllFields()) && assetTypeConfiguration.selectedLinearAsset.isDirty()));
      };

      updateStatus(element);

      eventbus.on(events('valueChanged'), function() {
        updateStatus(element);
      });

      return {
        element: element
      };
    };

    var CancelButton = function(assetTypeConfiguration) {

      var element = $('<button />').prop('disabled', !assetTypeConfiguration.selectedLinearAsset.isDirty()).addClass('cancel btn btn-secondary').text('Peruuta').click(function() {
        currentLane = parseInt(currentLane.toString()[0] + '1');
        assetTypeConfiguration.selectedLinearAsset.cancel();
      });

      eventbus.on(events('valueChanged'), function() {
        if(assetTypeConfiguration.selectedLinearAsset.isDirty()){
          $('.cancel').prop('disabled', false);
        }else{
          $('.cancel').prop('disabled', true);
        }
      });

      return {
        element: element
      };
    };

    var VerificationButton = function(assetTypeConfiguration) {
      var visible = (assetTypeConfiguration.isVerifiable && !_.isNull(assetTypeConfiguration.selectedLinearAsset.getId()) && assetTypeConfiguration.selectedLinearAsset.count() === 1);

      var element = visible ? $('<button />').prop('disabled', me.isSaveable()).addClass('verify btn btn-primary').text('Merkitse tarkistetuksi').click(function() {
        assetTypeConfiguration.selectedLinearAsset.verify();
      }) : '';

      var updateStatus = function() {
        if(!_.isEmpty(element))
          element.prop('disabled', me.isSaveable());
      };

      updateStatus();

      eventbus.on(events('valueChanged'), function() {
        updateStatus();
      });

      return {
        element: element
      };
    };

    jQuery.fn.showElement = function(visible) {
      var toggle = visible ? 'visible' : 'hidden';
      return this.css('visibility', toggle);
    };
  };
})(this);