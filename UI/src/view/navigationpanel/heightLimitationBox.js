(function (root){
  root.HeightLimitationBox = function (assetConfig) {
    PointAssetBox.call(this, assetConfig);
    var me = this;

    this.labeling = function () {
      var heightLimitValues = [
        [1, 'Suurin sallittu korkeus']
      ];

      return _(assetConfig.legendValues).map(function (val) {
        return '<div class="legend-entry">' +
          '  <div class="label">' +
          '    <span>' + val.label + '</span> ' +
          '    <img class="symbol" src="' + val.symbolUrl + '"/>' +
          '  </div>' +
          '</div>';
      }).join('').concat(_.map(heightLimitValues, function(heightLimit) {
        return '<div class="panel-legend limitation-label-legend">' +
          '  <div class="labeling-entry">' +
          '   <div class="limitation-' + heightLimit[0] + '">' + heightLimit[1] +
          '   </div>' +
          '  </div>' +
          '</div>';
      }).join('')) + '</div>';
    };

    var element = $('<div class="panel-group point-asset ' +  _.kebabCase(assetConfig.layerName) + '"/>');

    function show() {
      me.getShow();
    }

    function hide() {
      element.hide();
    }

    this.getElement = function () {
      return element;
    };

    this.show = show;
    this.hide = hide;
  };
})(this);
