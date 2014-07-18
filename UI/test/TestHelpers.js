define(function() {
  var restartApplication = function(callback, fakeBackend) {
    eventbus.once('application:initialized', callback);
    Application.restart(fakeBackend);
  };

  var fakeBackend = function(assetsData) {
    return Backend.withRoadLinkData(RoadLinkTestData.generate())
      .withUserRolesData(UserRolesTestData.generate())
      .withEnumeratedPropertyValues(EnumeratedPropertyValuesTestData.generate())
      .withApplicationSetupData(ApplicationSetupTestData.generate())
      .withConfigurationData(ConfigurationTestData.generate())
      .withAssetPropertyNamesData(AssetPropertyNamesTestData.generate())
      .withAssetsData(assetsData);
  };

  var clickMarker = function(id, event) {
    var asset = AssetsModel.getAsset(id);
    if (asset) { asset.mouseDownHandler(event); }
  };

  return {
    restartApplication: restartApplication,
    fakeBackend: fakeBackend,
    clickMarker: clickMarker
  };
});