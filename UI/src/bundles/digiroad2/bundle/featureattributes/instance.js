/**
 * @class Oskari.digiroad2.bundle.featureattributes.FeatureAttributesBundleInstance
 *
 */
Oskari.clazz.define("Oskari.digiroad2.bundle.featureattributes.FeatureAttributesBundleInstance",

    /**
     * @method create called automatically on construction
     * @static
     */
        function() {
        this.sandbox = null;
        this.started = false;
        this.mediator = null;
        this._enumeratedPropertyValues = null;
        this._featureDataAssetId = null;
    }, {
        /**
         * @static
         * @property __name
         */
        __name : 'FeatureAttributes',

        /**
         * @method getName
         * @return {String} the name for the component
         */
        getName : function() {
            return this.__name;
        },
        /**
         * @method setSandbox
         * @param {Oskari.mapframework.sandbox.Sandbox} sandbox
         * Sets the sandbox reference to this component
         */
        setSandbox : function(sbx) {
            this.sandbox = sbx;
        },
        /**
         * @method getSandbox
         * @return {Oskari.mapframework.sandbox.Sandbox}
         */
        getSandbox : function() {
            return this.sandbox;
        },
        /**
         * @method update
         * implements BundleInstance protocol update method - does nothing atm
         */
        update : function() {
        },
        /**
         * @method start
         * implements BundleInstance protocol start methdod
         */
        start : function() {
            var me = this;
            if(me.started) {
                return;
            }
            me.started = true;
            // Should this not come as a param?
            var sandbox = Oskari.$('sandbox');
            sandbox.register(me);
            me.setSandbox(sandbox);

            for(var p in me.eventHandlers) {
                if(p) {
                    sandbox.registerForEventByName(me, p);
                }
            }

            sandbox.addRequestHandler('FeatureAttributes.ShowFeatureAttributesRequest', this.requestHandlers.showFeatureAttributesHandler);
            sandbox.addRequestHandler('FeatureAttributes.CollectFeatureAttributesRequest', this.requestHandlers.collectFeatureAttributesHandler);
        },
        /**
         * @method init
         * implements Module protocol init method - initializes request handlers
         */
        init : function(options) {
            var me = this;
            this.requestHandlers = {
                showFeatureAttributesHandler : Oskari.clazz.create('Oskari.digiroad2.bundle.featureattributes.request.ShowFeatureAttributesRequestHandler', this),
                collectFeatureAttributesHandler : Oskari.clazz.create('Oskari.digiroad2.bundle.featureattributes.request.CollectFeatureAttributesRequestHandler', this)
            };

            _.templateSettings = {
                interpolate: /\{\{(.+?)\}\}/g
            };

            me._featureDataWrapper = _.template('<div class="featureAttributesHeader">{{header}}</div>' +
                                                '<div class="featureAttributesWrapper">' +
                                                    '<div class="streetView">{{streetView}}</div>' +
                                                    '<div class="formContent">{{attributes}}</div>' +
                                                '</div>');

            me._streetViewTemplate  = _.template(
                '<a target="_blank" href="http://maps.google.com/?ll={{wgs84Y}},{{wgs84X}}&cbll={{wgs84Y}},{{wgs84X}}&cbp=12,20.09,,0,5&layer=c&t=m">' +
                    '<img src="http://maps.googleapis.com/maps/api/streetview?size=360x180&location={{wgs84Y}}' +
                    ', {{wgs84X}}&fov=110&heading={{heading}}&pitch=-10&sensor=false">' +
                '</a>');

            me._featureDataTemplate = _.template('<div class="formAttributeContentRow">' +
                                                    '<div class="formLabels">{{propertyName}}</div>' +
                                                    '<div class="formAttributeContent">{{propertyValue}}</div>' +
                                                 '</div>');
            me._featureDataTemplateText = _.template('<div class="formAttributeContentRow">' +
                                                        '<div class="formLabels">{{propertyName}}</div>' +
                                                         '<div class="formAttributeContent">' +
                                                            '<input class="featureAttributeText" type="text"' +
                                                            ' data-propertyId="{{propertyId}}" name="{{propertyName}}"' +
                                                            ' value="{{propertyDisplayValue}}">' +
                                                         '</div>' +
                                                     '</div>');
            me._featureDataTemplateDate = _.template('<div class="formAttributeContentRow">' +
                                                        '<div class="formLabels">{{propertyName}}</div>' +
                                                        '<div class="formAttributeContent">' +
                                                            '<input class="featureAttributeDate" type="text"' +
                                                                ' data-propertyId="{{propertyId}}" name="{{propertyName}}"' +
                                                                ' value="{{propertyDisplayValue}}"/><span class="attributeFormat">pp.kk.vvvv</span>' +
                                                        '</div>' +
                                                     '</div>');
            me._featureDataTemplateNA = _.template('<div class="formAttributeContentRow">' +
                                                    '<div class="formLabels">{{propertyName}}</div>' +
                                                    '<div class="featureAttributeNA">{{propertyValue}}</div>' +
                                                 '</div>');

            me._featureDataTemplateChoice = _.template('<option {{selectedValue}} value="{{propertyValue}}">{{propertyDisplayValue}}</option>');
            me._getPropertyValues();

            me._backend = options.backend || window.Backend;

            return null;
        },
        showAttributes : function(id, point) {
            var me = this;
            me._featureDataAssetId = id;
            me._backend.getAsset(id, function(data) {
                var featureData = me._makeContent(data.propertyData);
                var streetView =  me._streetViewTemplate({ "wgs84X":point.x, "wgs84Y":point.y, "heading" : point.heading});
                var featureAttributes = me._featureDataWrapper({ header : id, streetView : streetView, attributes : featureData });

                jQuery("#featureAttributes").html(featureAttributes);
                jQuery(".featureAttributeText").on("blur", function() {
                    var data = jQuery(this);
                    me._saveTextData(me._propertyValuesOfTextElement(data), data.attr('data-propertyId'));
                });
                jQuery(".featureattributeChoice").on("change", function() {
                    var data = jQuery(this);
                    var propertyValue = [];
                    var values = data.val();
                    var name = data.attr('name');

                    _.forEach(values,
                        function(value) {
                            propertyValue.push({
                                "propertyValue" : Number(value),
                                "propertyDisplayValue" : name
                            });
                        }
                    );

                    me._saveTextData(propertyValue, data.attr('data-propertyId'));
                });
                var dateAttribute = jQuery(".featureAttributeDate");
                dateAttribute.on("blur", function() {
                    var data = jQuery(this);
                    var propertyValue = [];
                    if(!_.isEmpty(data.val())) {
                        propertyValue.push({
                            "propertyValue" : 0,
                            "propertyDisplayValue" : dateutil.finnishToIso8601(data.val())
                        });
                    }
                    me._saveTextData(propertyValue, data.attr('data-propertyId'));
                });
                dateAttribute.each(function(i, element) {
                    dateutil.addFinnishDatePicker(element);
                });
            });
        },
        collectAttributes: function(successCallback) {
            var me = this;
            me._backend.getAssetTypeProperties(10, assetTypePropertiesCallback);
            function assetTypePropertiesCallback(properties) {
                var featureAttributesElement = jQuery('#featureAttributes');
                var featureData = me._makeContent(properties);
                var featureAttributesMarkup = me._featureDataWrapper({ header : 0, streetView : null, attributes : featureData });
                featureAttributesMarkup += '<button class="save">Tallenna</button>';
                featureAttributesElement.html(featureAttributesMarkup);
                featureAttributesElement.find('button.save').on('click', function() {
                    var inputElements = featureAttributesElement.find('input');
                    var attributeCollection = _.map(inputElements, function(inputElement) {
                        var jqElement = jQuery(inputElement);
                        return {
                          propertyId: jqElement.attr('data-propertyId'),
                          propertyValues: me._propertyValuesOfTextElement(jqElement)
                        };
                    });
                    successCallback(attributeCollection);
                });
            }
        },
        _propertyValuesOfTextElement: function(element) {
            return [{
                "propertyValue" : 0,
                "propertyDisplayValue" : element.val()
            }];
        },
        _getPropertyValues: function() {
            var me = this;
            jQuery.getJSON("/api/enumeratedPropertyValues/10", function(data) {
                 me._enumeratedPropertyValues = data;
                 return data;
                })
                .fail(function() {
                    console.log( "error" );
                });
        },
        _saveTextData: function(propertyValue, propertyId) {
            var me = this;
            me._backend.putAssetPropertyValue(this._featureDataAssetId, propertyId, propertyValue, successFunction);
            function successFunction() {
                var eventBuilder = me.getSandbox().getEventBuilder('featureattributes.FeatureAttributeChangedEvent');
                var event = eventBuilder(propertyValue);
                me.getSandbox().notifyAll(event);
                console.log("done");
            }
        },
        _makeContent: function(contents) {
            var me = this;
            var html = "";
            _.forEach(contents,
                function (feature) {
                    if (feature.propertyType == "text") {
                        feature.propertyValue = "";
                        feature.propertyDisplayValue = "";
                        if (feature.values[0]) {
                            feature.propertyValue = feature.values[0].propertyValue;
                            feature.propertyDisplayValue = feature.values[0].propertyDisplayValue;
                        }
                        html += me._featureDataTemplateText(feature);
                    } else if (feature.propertyType == "single_choice") {
                        feature.propertyValue = me._getSelect(feature.propertyName, feature.values, feature.propertyId, '');
                        html += me._featureDataTemplate(feature);
                    } else if (feature.propertyType == "multiple_choice") {
                        feature.propertyValue = me._getSelect(feature.propertyName, feature.values, feature.propertyId, 'multiple');
                        html += me._featureDataTemplate(feature);
                    } else if (feature.propertyType == "date") {
                        feature.propertyValue = "";
                        feature.propertyDisplayValue = "";
                        if (feature.values[0]) {
                            feature.propertyValue = dateutil.iso8601toFinnish(feature.values[0].propertyDisplayValue);
                            feature.propertyDisplayValue = dateutil.iso8601toFinnish(feature.values[0].propertyDisplayValue);
                        }
                        html += me._featureDataTemplateDate(feature);
                    }  else {
                        feature.propertyValue ='Ei toteutettu';
                        html += me._featureDataTemplateNA(feature);
                    }
                }
            );
            return html;
        },
        _getSelect: function(name, values, propertyId, multiple) {
            var me = this;
            var options = '<select data-propertyId="'+propertyId+'" name="'+name+'" class="featureattributeChoice" ' + multiple +'>';
            var valuesNro = _.map(values, function(x) { return x.propertyValue;});

            var propertyValues = _.find(me._enumeratedPropertyValues,
                function(property) {
                    if (property.propertyId === propertyId) {
                        return property;
                    }
                }
            );

            _.forEach(propertyValues.values,
                function(optionValue) {
                    var selectedValue ='';

                    if (_.contains(valuesNro, optionValue.propertyValue)) {
                        selectedValue = 'selected="true" ';
                    }

                    optionValue.selectedValue = selectedValue;
                    options +=  me._featureDataTemplateChoice(optionValue);
                }
            );

            options +="</select>";
            return options;
        },
        /**
         * @method onEvent
         * @param {Oskari.mapframework.event.Event} event a Oskari event object
         * Event is handled forwarded to correct #eventHandlers if found or discarded if not.
         */
        onEvent : function(event) {
            var me = this;
            var handler = me.eventHandlers[event.getName()];
            if(!handler) {
                return;
            }

            return handler.apply(this, [event]);
        },
        /**
         * @property {Object} eventHandlers
         * @static
         */
        eventHandlers : {
            'infobox.InfoBoxClosedEvent': function (event) {
                this._closeFeatures(event);
            }

        },
        _closeFeatures: function (event) {
            jQuery("#featureAttributes").html('');
        },
        /**
         * @method stop
         * implements BundleInstance protocol stop method
         */
        stop : function() {
            var me = this;
            var sandbox = this.sandbox;
            for(var p in me.eventHandlers) {
                if(p) {
                    sandbox.unregisterFromEventByName(me, p);
                }
            }
            me.sandbox.unregister(me);
            me.started = false;
        }
    }, {
        /**
         * @property {String[]} protocol
         * @static
         */
        protocol : ['Oskari.bundle.BundleInstance', 'Oskari.mapframework.module.Module']
    });