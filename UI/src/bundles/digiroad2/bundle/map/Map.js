Oskari.clazz.define('Oskari.digiroad2.bundle.map.Map',
  function() {
    this.mapModule = null;
  }, {
    getName: function() {
    },
    getMapModule: function() {
      return this.mapModule;
    },
    setMapModule: function(mapModule) {
      this.mapModule = mapModule;
    },
    register: function() {
      this.getMapModule().setLayerPlugin('map', this);
    },
    unregister: function() {
      this.getMapModule().setLayerPlugin('map', null);
    },
    init: function(sandbox) {
      new MapView(this.mapModule.getMap(), sandbox);

      // register domain builder
      var mapLayerService = sandbox.getService('Oskari.mapframework.service.MapLayerService');
      if (mapLayerService) {
        mapLayerService.registerLayerModel('map', 'Oskari.digiroad2.bundle.map.domain.BusStopLayer');
      }
      sandbox.postRequestByName('RearrangeSelectedMapLayerRequest', ['base_35', 0]);
    },
    startPlugin: function(sandbox) {
      sandbox.register(this);
    },
    start: function(sandbox) {
    },
    getOLMapLayers: function(layer) {
    }
  }, {
    'protocol': ["Oskari.mapframework.module.Module", "Oskari.mapframework.ui.module.common.mapmodule.Plugin"]
  });
