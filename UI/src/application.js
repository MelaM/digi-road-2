var RoadCollection = function(backend) {
  var roadLinks = [];

  this.fetch = function(boundingBox) {
    backend.getRoadLinks(boundingBox, function(data) {
      roadLinks = data;
    });
  };

  this.getAll = function() {
    return roadLinks;
  };

  this.activate = function(road) {
    eventbus.trigger('road:active', road.roadLinkId);
  };
};

(function(application) {
  Oskari.setLang('fi');
  Oskari.setLoaderMode('dev');
  var appSetup;
  var appConfig;
  var localizedStrings;
  var assetUpdateFailedMessage = 'Tallennus epäonnistui. Yritä hetken kuluttua uudestaan.';

  var assetIdFromURL = function() {
    var matches = window.location.hash.match(/(\d+)(.*)/);
    if (matches) {
      return {externalId: parseInt(matches[1], 10), keepPosition: _.contains(window.location.hash, 'keepPosition=true')};
    }
  };

  var indicatorOverlay = function() {
    jQuery('.container').append('<div class="spinner-overlay"><div class="spinner"></div></div>');
  };

  var selectAssetFromAddressBar = function() {
    var data = assetIdFromURL();
    if (data && data.externalId) {
      selectedAssetModel.changeByExternalId(data.externalId);
    }
  };

  var hashChangeHandler = function() {
    $(window).off('hashchange', hashChangeHandler);
    var oldHash = window.location.hash;

    selectAssetFromAddressBar(); // Empties the hash, so we need to set it back to original state.

    window.location.hash = oldHash;
    $(window).on('hashchange', hashChangeHandler);
  };

  var bindEvents = function(backend, models) {
    eventbus.on('application:readOnly tool:changed asset:closed asset:placed', function() {
      window.location.hash = '';
    });

    $(window).on('hashchange', hashChangeHandler);

    eventbus.on('asset:saving asset:creating', function() {
      indicatorOverlay();
    });

    eventbus.on('asset:fetched asset:created', function(asset) {
      jQuery('.spinner-overlay').remove();
      var keepPosition = 'true';
      var data = assetIdFromURL();
      if (data && !data.keepPosition) {
        eventbus.trigger('coordinates:selected', { lat: asset.lat, lon: asset.lon });
        keepPosition = 'false';
      }
      window.location.hash = '#/asset/' + asset.externalId + '?keepPosition=' + keepPosition;
    });

    eventbus.on('asset:saved', function() {
      jQuery('.spinner-overlay').remove();
    });

    eventbus.on('asset:updateFailed asset:creationFailed', function() {
      jQuery('.spinner-overlay').remove();
      alert(assetUpdateFailedMessage);
    });

    eventbus.on('applicationSetup:fetched', function(setup) {
      appSetup = setup;
      startApplication(backend, models);
    });

    eventbus.on('configuration:fetched', function(config) {
      appConfig = config;
      startApplication(backend, models);
    });

    eventbus.on('assetPropertyNames:fetched', function(assetPropertyNames) {
      localizedStrings = assetPropertyNames;
      window.localizedStrings = assetPropertyNames;
      startApplication(backend, models);
    });

    eventbus.on('confirm:show', function() { new Confirm(); });

    eventbus.once('assets:all-updated', selectAssetFromAddressBar);
  };

  var setupMap = function(backend, models) {
    var map = Oskari.getSandbox()._modulesByName.MainMapModule.getMap();

    var roadCollection = new RoadCollection(backend);
    var layers = {
      road: new RoadLayer(map, roadCollection),
      asset: new AssetLayer(map, roadCollection),
      speedLimit: new SpeedLimitLayer(map, applicationModel, models.speedLimitsCollection, models.selectedSpeedLimit)
    };
    new MapView(map, layers);
    map.setBaseLayer(_.first(map.getLayersBy('layer', 'taustakartta')));

    new ZoomBox(map, $('.mapplugins.bottom.left .mappluginsContent'));
  };

  var startApplication = function(backend, models) {
    // check that both setup and config are loaded 
    // before actually starting the application
    if (appSetup && appConfig && localizedStrings) {
      var app = Oskari.app;
      app.setApplicationSetup(appSetup);
      app.setConfiguration(appConfig);
      app.startApplication(function() {
        setupMap(backend, models);
        eventbus.trigger('application:initialized');
      });
    }
  };

  application.start = function(customBackend) {
    var backend = customBackend || new Backend();
    var speedLimitsCollection = new SpeedLimitsCollection(backend);
    var selectedSpeedLimit = new SelectedSpeedLimit(backend, speedLimitsCollection);
    var models = {
      speedLimitsCollection: speedLimitsCollection,
      selectedSpeedLimit: selectedSpeedLimit
    };
    bindEvents(backend, models);
    window.assetsModel = new AssetsModel(backend);
    window.selectedAssetModel = SelectedAssetModel.initialize(backend);
    window.applicationModel = new ApplicationModel(selectedAssetModel, selectedSpeedLimit);
    ActionPanel.initialize(backend, selectedSpeedLimit);
    AssetForm.initialize(backend);
    SpeedLimitForm.initialize();
    backend.getApplicationSetup();
    backend.getConfiguration(assetIdFromURL());
    backend.getAssetPropertyNames();
  };

  application.restart = function(backend) {
    appSetup = undefined;
    appConfig = undefined;
    localizedStrings = undefined;
    this.start(backend);
  };

}(window.Application = window.Application || {}));
