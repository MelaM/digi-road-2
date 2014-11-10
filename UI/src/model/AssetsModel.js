(function(root) {
  root.AssetsModel = function(backend) {
    var assets = {};
    var validityPeriods = {
      current: true,
      future: false,
      past: false
    };
    var filterNonExistingAssets = function(assets, existingAssets) {
      return _.reject(assets, function(asset) {
        return _.has(existingAssets, asset.id.toString());
      });
    };
    var selectedValidityPeriods = function(validityPeriods) {
      return _.keys(_.pick(validityPeriods, function(selected) {
        return selected;
      }));
    };

    var refreshAssets = function(mapMoveEvent) {
      backend.getAssetsWithCallback(mapMoveEvent.bbox, function(backendAssets) {
        _.each(backendAssets, function(asset) {
          if (asset.floating) {
            console.log('mass transit stop with external id: ' + asset.externalId + ' is floating');
          }
        });
        if (mapMoveEvent.hasZoomLevelChanged) {
          eventbus.trigger('assets:all-updated', backendAssets);
        } else {
          eventbus.trigger('assets:new-fetched', filterNonExistingAssets(backendAssets, assets));
        }
      });
    };

    return {
      insertAsset: function(asset, assetId) {
        assets[assetId] = asset;
      },
      getAsset: function(assetId) {
        return assets[assetId];
      },
      destroyAsset: function(assetId) {
        assets = _.omit(assets, assetId.toString());
      },
      getAssets: function() {
        return assets;
      },
      fetchAssets: function(boundingBox) {
        backend.getAssets(boundingBox);
      },
      refreshAssets: refreshAssets,
      insertAssetsFromGroup: function(assetGroup) {
        _.each(assetGroup, function(asset) {
          assets[asset.data.id.toString()] = asset;
        });
      },
      destroyGroup: function(assetIds) {
        var destroyedAssets = _.pick(assets, assetIds);
        assets = _.omit(assets, assetIds);
        eventbus.trigger('assetGroup:destroyed', destroyedAssets);
      },
      destroyAssets: function() {
        assets = {};
      },
      selectValidityPeriod: function(validityPeriod, isSelected) {
        if (validityPeriods[validityPeriod] !== isSelected) {
          validityPeriods[validityPeriod] = isSelected;
          eventbus.trigger('validityPeriod:changed', selectedValidityPeriods(validityPeriods));
        }
      },
      getValidityPeriods: function() {
        return validityPeriods;
      },
      selectedValidityPeriodsContain: function(validityPeriod) {
        return validityPeriods[validityPeriod];
      }
    };
  };
})(this);
