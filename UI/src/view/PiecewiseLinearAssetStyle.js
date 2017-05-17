(function(root) {
  root.PiecewiseLinearAssetStyle = function(applicationModel) {

    var expirationRules = [
      new StyleRule().where('expired').is(true).use({ stroke : { color: '#7f7f7c'}}),
      new StyleRule().where('expired').is(false).use({ stroke : { color: '#ff0000'}})
    ];

    var zoomLevelRules = [
      new StyleRule().where('zoomLevel').is(9).use({ stroke: {width: 3 }}),
      new StyleRule().where('zoomLevel').is(10).use({ stroke: {width: 5 }}),
      new StyleRule().where('zoomLevel').is(11).use({ stroke: {width: 8 }}),
      new StyleRule().where('zoomLevel').is(12).use({ stroke: {width: 10 }}),
      new StyleRule().where('zoomLevel').is(13).use({ stroke: {width: 10 }}),
      new StyleRule().where('zoomLevel').is(14).use({ stroke: {width: 14 }}),
      new StyleRule().where('zoomLevel').is(15).use({ stroke: {width: 14 }})
    ];

    var oneWayRules = [
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(9).use({ stroke: {width: 2 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(10).use({ stroke: {width: 4 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(11).use({ stroke: {width: 4 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(12).use({ stroke: {width: 5 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(13).use({ stroke: {width: 5 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(14).use({ stroke: {width: 8 }}),
      new StyleRule().where('sideCode').isIn([2,3]).and('zoomLevel', applicationModel.zoom).is(15).use({ stroke: {width: 8 }})
    ];

    var featureTypeRules = [
      new StyleRule().where('type').is('cutter').use({ icon: {  src: 'images/cursor-crosshair.svg'}})
    ];

    var browseStyleProvider = new StyleRuleProvider({ stroke : { opacity: 0.7 }});
    browseStyleProvider.addRules(expirationRules);
    browseStyleProvider.addRules(zoomLevelRules);
    browseStyleProvider.addRules(oneWayRules);
    browseStyleProvider.addRules(featureTypeRules);

    var lineFeatures = function(linearAssets) {
      return _.flatten(_.map(linearAssets, function(linearAsset) {
        var points = _.map(linearAsset.points, function(point) {
          return [point.x, point.y];
        });
        var feature = new ol.Feature(new ol.geom.LineString(points));
        feature.setProperties(linearAsset);
        return feature;
      }));
    };

    var getNewFeatureProperties = function(linearAssets){
      var linearAssetsWithType = _.map(linearAssets, function(limit) {
        var expired = _.isUndefined(limit.value);
        return _.merge({}, limit, { type: 'line', expired: expired });
      });
      var offsetBySideCode = function(linearAsset) {
        return GeometryUtils.offsetBySideCode(applicationModel.zoom.level, linearAsset);
      };
      var linearAssetsWithAdjustments = _.map(linearAssetsWithType, offsetBySideCode);
      var sortedAssets = _.sortBy(linearAssetsWithAdjustments, function(asset) {
        return asset.expired ? -1 : 1;
      });
      return sortedAssets;
    };

    var renderFeatures = function(linearAssets) {
      return lineFeatures(getNewFeatureProperties(linearAssets));
    };

    return {
      browsingStyleProvider: browseStyleProvider,
      vectorOpacity: 0.15,
      renderFeatures: renderFeatures
    };
  };
})(this);

