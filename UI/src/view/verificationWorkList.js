(function (root) {
  root.VerificationWorkList = function(){
    WorkListView.call(this);
    var me = this;
    this.initialize = function(){
      me.bindEvents();
    };

    this.bindEvents = function() {
      eventbus.on('verificationList:select', function(layerName, listP) {
        $('.container').hide();
        $('#work-list').show();
        $('body').addClass('scrollable');
        generateWorkList(layerName, listP);
      });
    };

    var generateWorkList = function(layerName, listP) {
      var title = 'Vanhentuneiden kohteiden lista';
      $('#work-list').html('' +
        '<div style="overflow: auto;">' +
        '<div class="page">' +
        '<div class="content-box">' +
        '<header>' + title +
        '<a class="header-link" href="#' + layerName + '">Sulje lista</a>' +
        '</header>' +
        '<div class="work-list">' +
        '</div>' +
        '</div>' +
        '</div>'
      );
      me.addSpinner();
      listP.then(function(assets) {
        var unverifiedAssets = _.map(assets, _.partial(me.workListItemTable, layerName, false));
        $('#work-list .work-list').html(unverifiedAssets);
        me.removeSpinner();
      });
    };
  };
})(this);
