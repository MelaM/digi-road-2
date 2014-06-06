(function (selectedAssetModel){
    selectedAssetModel.initialize = function(backend) {
        var usedKeysFromFetchedAsset = ['assetTypeId', 'bearing', 'lat', 'lon', 'roadLinkId'];
        var assetHasBeenModified = false;
        var currentAsset = {};
        var changedProps = [];

        var reset = function() {
            assetHasBeenModified = false;
            currentAsset = {};
            changedProps = [];
            eventbus.trigger('asset:closed');
        };

        eventbus.on('asset:unselected', function() {
            reset();
        });

        eventbus.on('asset:placed', function(asset) {
            currentAsset = asset;

            // TODO: copy paste
            var transformPropertyData = function(propertyData) {
                var transformValues = function(publicId, values) {
                    var transformValue = function(value) {
                        return {
                            propertyValue: value.propertyValue,
                            propertyDisplayValue: publicId.publicId
                        };
                    };

                    return _.map(values.values, transformValue);
                };
                var transformProperty = function(property) {
                    return _.merge(
                        {},
                        _.pick(property, 'publicId'),
                        {
                            values: transformValues(_.pick(property, 'publicId'), _.pick(property, 'values'))
                        });
                };
                return {
                    properties: _.map(propertyData.propertyData, transformProperty)
                };
            };
            eventbus.once('assetTypeProperties:fetched', function(properties) {
                currentAsset.propertyData = properties;
                currentAsset.payload = _.merge({ assetTypeId: 10 }, _.pick(currentAsset, usedKeysFromFetchedAsset), transformPropertyData(_.pick(currentAsset, 'propertyData')));
                changedProps = currentAsset.payload.properties;
                eventbus.trigger('asset:initialized', currentAsset);
            });
            backend.getAssetTypeProperties(10);
        }, this);

        eventbus.on('asset:moved', function(position) {
            currentAsset.payload.bearing = position.bearing;
            currentAsset.payload.lon = position.lon;
            currentAsset.payload.lat = position.lat;
            currentAsset.payload.roadLinkId = position.roadLinkId;
            assetHasBeenModified = true;
        });

        eventbus.on('assetPropertyValue:changed', function(changedProperty) {
            changedProps = _.reject(changedProps, function(x){
                return x.publicId === changedProperty.propertyData.publicId;
            });
            changedProps.push(changedProperty.propertyData);
            currentAsset.payload.properties = changedProps;
            assetHasBeenModified = true;
        });

        eventbus.on('asset:cancelled application:readOnly', function(){
           if (currentAsset.id) {
               backend.getAsset(currentAsset.id, true);
           }
        });

        eventbus.on('validityPeriod:changed', function(validityPeriods) {
            if (currentAsset && !_.contains(validityPeriods, currentAsset.validityPeriod)) {
                reset();
            }
        });

        eventbus.on('asset:saved asset:created asset:cancelled', function() {
            changedProps = [];
            assetHasBeenModified = false;
        });
        eventbus.on('asset:created', function(asset) {
           currentAsset.id = asset.id;
        });

        eventbus.on('asset:fetched', function(asset) {
            // TODO: copy paste
            var transformPropertyData = function(propertyData) {
                var transformValues = function(publicId, values) {
                    var transformValue = function(value) {
                        return {
                            propertyValue: value.propertyValue,
                            propertyDisplayValue: publicId.publicId
                        };
                    };
                    return _.map(values.values, transformValue);
                };
                var transformProperty = function(property) {
                    return _.merge(
                        {},
                        _.pick(property, 'publicId'),
                        {
                            values: transformValues(_.pick(property, 'publicId'), _.pick(property, 'values'))
                        });
                };
                return {
                    properties: _.map(propertyData.propertyData, transformProperty)
                };
            };
            currentAsset.id = asset.id;
            currentAsset.payload = _.merge({}, _.pick(asset, usedKeysFromFetchedAsset), transformPropertyData(_.pick(asset, 'propertyData')));
            currentAsset.validityPeriod = asset.validityPeriod;
        });

        var save = function() {
            if(currentAsset.id === undefined){
                backend.createAsset(currentAsset.payload);
            } else {
                currentAsset.payload.id = currentAsset.id;
                backend.updateAsset(currentAsset.id, currentAsset.payload);
            }
        };

        return { reset: reset,
                 save: save,
                 isDirty: function() { return assetHasBeenModified; }};
    };

})(window.SelectedAssetModel = window.SelectedAssetModel || {});