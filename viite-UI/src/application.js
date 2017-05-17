(function(application) {
  application.start = function(customBackend, withTileMaps, isExperimental) {
    var backend = customBackend || new Backend();
    var tileMaps = _.isUndefined(withTileMaps) ? true : withTileMaps;
    var roadCollection = new RoadCollection(backend);
    var roadAddressProjectCollection = new RoadAddressProjectCollection(backend);
    var selectedLinkProperty = new SelectedLinkProperty(backend, roadCollection);
    var linkPropertiesModel = new LinkPropertiesModel();
    var instructionsPopup = new InstructionsPopup($('.digiroad2'));

    var models = {
      roadCollection: roadCollection,
      roadAddressProjectCollection: roadAddressProjectCollection,
      selectedLinkProperty: selectedLinkProperty,
      linkPropertiesModel: linkPropertiesModel
    };

    bindEvents();
    window.applicationModel = new ApplicationModel([
      selectedLinkProperty]);

    EditModeDisclaimer.initialize(instructionsPopup);

    var assetGroups = groupAssets(
      linkPropertiesModel);

    var projectListModel = new ProjectListModel(roadAddressProjectCollection);

    NavigationPanel.initialize(
      $('#map-tools'),
      new SearchBox(
        instructionsPopup,
        new LocationSearch(backend, window.applicationModel)
      ),
      new ProjectSelectBox(projectListModel),
      assetGroups
    );

    backend.getUserRoles();
    backend.getStartupParametersWithCallback(function (startupParameters) {
      startApplication(backend, models, tileMaps, startupParameters);
    });
  };

  var startApplication = function(backend, models, withTileMaps, startupParameters) {
    setupProjections();
    fetch('components/WMTSCapabilities.xml', {credentials: "include"}).then(function(response) {
      return response.text();
    }).then(function(arcConfig) {
      var map = setupMap(backend, models, withTileMaps, startupParameters, arcConfig);
      new URLRouter(map, backend, models);
      eventbus.trigger('application:initialized');
    });
  };

  var localizedStrings;

  var indicatorOverlay = function() {
    jQuery('.container').append('<div class="spinner-overlay modal-overlay"><div class="spinner"></div></div>');
  };

  var createOpenLayersMap = function(startupParameters, layers) {
    var map = new ol.Map({
      target: 'mapdiv',
      layers: layers,
      view: new ol.View({
        center: [startupParameters.lon, startupParameters.lat],
        projection: 'EPSG:3067',
        zoom: startupParameters.zoom,
        resolutions: [2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5, 0.25, 0.125, 0.0625]
      })
    });
    map.setProperties({extent : [-548576, 6291456, 1548576, 8388608]});
    return map;
  };

  var setupMap = function(backend, models, withTileMaps, startupParameters, arcConfig) {
    var tileMaps = new TileMapCollection(map, arcConfig);

    var map = createOpenLayersMap(startupParameters, tileMaps.layers);

    var mapOverlay = new MapOverlay($('.container'));
    var styler = new Styler();
    var roadLayer = new RoadLayer3(map, models.roadCollection,styler,models.selectedLinkProperty);

    new LinkPropertyForm(models.selectedLinkProperty);

    new RoadAddressProjectForm(models.roadAddressProjectCollection);

    var layers = _.merge({
      road: roadLayer,
      linkProperty: new LinkPropertyLayer(map, roadLayer, models.selectedLinkProperty, models.roadCollection, models.linkPropertiesModel, applicationModel, styler)});

    var mapPluginsContainer = $('#map-plugins');
    new ScaleBar(map, mapPluginsContainer);
    new TileMapSelector(mapPluginsContainer);
    new ZoomBox(map, mapPluginsContainer);
    new CoordinatesDisplay(map, mapPluginsContainer);

    // Show environment name next to Digiroad logo
    $('#notification').append(Environment.localizedName());
    $('#notification').append(' Päivämäärä: ' + startupParameters.deploy_date);

    // Show information modal in integration environment (remove when not needed any more)
    if (Environment.name() === 'integration') {
      showInformationModal('Huom!<br>Tämä sivu ei ole enää käytössä.<br>Digiroad-sovellus on siirtynyt osoitteeseen <a href="https://extranet.liikennevirasto.fi/digiroad/" style="color:#FFFFFF;text-decoration: underline">https://extranet.liikennevirasto.fi/digiroad/</a>');
    }

    new MapView(map, layers, new InstructionsPopup($('.digiroad2')));

    applicationModel.moveMap(map.getView().getZoom(), map.getLayers().getArray()[0].getExtent());

    return map;
  };

  var setupProjections = function() {
    proj4.defs('EPSG:3067', '+proj=utm +zone=35 +ellps=GRS80 +units=m +no_defs');
  };

  function getSelectedPointAsset(pointAssets, layerName) {
    return _(pointAssets).find({ layerName: layerName }).selectedPointAsset;
  }

  function groupAssets(linkPropertiesModel) {

    var roadLinkBox = new RoadLinkBox(linkPropertiesModel);

    return [
      [roadLinkBox]
    ];

    function getLinearAsset(typeId) {
      var asset = _.find(linearAssets, {typeId: typeId});
      if (asset) {
        var legendValues = [asset.editControlLabels.disabled, asset.editControlLabels.enabled];
        return [new LinearAssetBox(asset.selectedLinearAsset, asset.layerName, asset.title, asset.className, legendValues)];
      }
      return [];
    }

    function getPointAsset(typeId) {
      var asset = _.find(pointAssets, {typeId: typeId});
      if (asset) {
        return [PointAssetBox(asset.selectedPointAsset, asset.title, asset.layerName, asset.legendValues)];
      }
      return [];
    }
  }

  // Shows modal with message and close button
  function showInformationModal(message) {
    $('.container').append('<div class="modal-overlay confirm-modal" style="z-index: 2000"><div class="modal-dialog"><div class="content">' + message + '</div><div class="actions"><button class="btn btn-secondary close">Sulje</button></div></div></div></div>');
    $('.confirm-modal .close').on('click', function() {
      $('.confirm-modal').remove();
    });
  }

  application.restart = function(backend, withTileMaps) {
    localizedStrings = undefined;
    this.start(backend, withTileMaps);
  };

  var bindEvents = function() {

    eventbus.on('linkProperties:saving', function() {
      indicatorOverlay();
    });

    eventbus.on('linkProperties:available', function() {
      jQuery('.spinner-overlay').remove();
    });

    eventbus.on('confirm:show', function() { new Confirm(); });
  };

}(window.Application = window.Application || {}));
