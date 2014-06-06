define(['chai', 'SelectedAssetModel'], function(chai) {
    var assert = chai.assert;
    describe('SelectedAssetModel', function() {
        var confirmDialogShown = false;
        var assetSentToBackend = {};
        var mockBackend = {
            updateAsset: function(id, data) {
                assetSentToBackend = {
                    id: id,
                    data: data
                };
            }
        };
        var model = SelectedAssetModel.initialize(mockBackend);

        before(function() {
            eventbus.on('confirm:show', function() { confirmDialogShown = true; });
        });

        var resetTest = function() {
            model.reset();
            confirmDialogShown = false;
            assetSentToBackend = {};
            eventbus.trigger('asset:fetched', createAsset());
        };

        var assetStateIsDirty = function() {
            return function() {
                it('asset state should be dirty', function() {
                    assert.equal(model.isDirty(), true);
                });
            };
        };

        var assetStateIsClean = function() {
            return function() {
                it('asset state should be clean', function() {
                    assert.equal(model.isDirty(), false);
                });
            };
        };

        describe('when asset is moved', function() {
            before(function() {
                resetTest();
                eventbus.trigger('asset:moved',{
                    lon: 1,
                    lat: 1,
                    bearing: 180,
                    roadLinkId: 3
                });
            });

            describe('and another asset is selected', assetStateIsDirty());

            describe('and changes are saved', function() {
                before(function() {
                    confirmDialogShown = false;
                    eventbus.trigger('asset:saved');
                });

                describe('and another asset is selected', assetStateIsClean());
            });
        });

        describe('when asset is moved', function() {
            before(function() {
                resetTest();
                eventbus.trigger('asset:moved',{
                    lon: 1,
                    lat: 2,
                    bearing: 90,
                    roadLinkId: 4
                });
            });

            describe('and changes are saved', function() {
                before(function() {
                    eventbus.trigger('asset:saved');
                });

                describe('and another asset is selected', assetStateIsClean());
            });
        });

        describe('when asset is not moved', function () {
            before(function() {
                resetTest();
            });

            describe('and another asset is selected', assetStateIsClean());
        });

        describe('when asset no longer falls on selected validity period', function() {
            var triggeredEvents = [];
            before(function() {
                eventbus.on('all', function(eventName) {
                    triggeredEvents.push(eventName);
                });
                eventbus.trigger('validityPeriod:changed', ['future', 'past']);
            });

            it('closes asset', function() {
                assert.include(triggeredEvents, 'asset:closed');
                assert.lengthOf(triggeredEvents, 2);
            });
        });

        function createAsset() {
            return {
                assetTypeId: 10,
                bearing: 80,
                externalId: 1,
                id: 300000,
                imageIds: ['2_1398341376263'],
                lat: 6677267.45072414,
                lon: 374635.608258218,
                municipalityNumber: 235,
                propertyData: [{
                    id: 0,
                    localizedName: 'Vaikutussuunta',
                    propertyType: 'single_choice',
                    propertyUiIndex: 65,
                    propertyValue: '2',
                    publicId: 'vaikutussuunta',
                    required: false,
                    values: [{
                        imageId: null,
                        propertyDisplayValue: 'Digitointisuuntaan',
                        propertyValue: '2'}
                    ]}, {
                    id: 200,
                    localizedName: 'Pysäkin tyyppi',
                    propertyType: 'multiple_choice',
                    propertyUiIndex: 90,
                    propertyValue: '<div data-publicId="pysakin_tyyppi" name="pysakin_tyyppi" class="featureattributeChoice"><input  type="checkbox" value="5"></input><label for="pysakin_tyyppi_5">Virtuaalipysäkki</label><br/><input  type="checkbox" value="1"/><label for="pysakin_tyyppi_1">Raitiovaunu</label><br/><input checked  type="checkbox" value="2"/><label for="pysakin_tyyppi_2">Linja-autojen paikallisliikenne</label><br/><input  type="checkbox" value="3"/><label for="pysakin_tyyppi_3">Linja-autojen kaukoliikenne</label><br/><input  type="checkbox" value="4"/><label for="pysakin_tyyppi_4">Linja-autojen pikavuoro</label><br/></div>',
                    publicId: 'pysakin_tyyppi',
                    required: true,
                    values: [{
                        imageId: '2_1398341376263',
                        propertyDisplayValue: 'Linja-autojen paikallisliikenne',
                        propertyValue: '2'
                    }, {
                        imageId: '3_1398341376270',
                        propertyDisplayValue: 'Linja-autojen kaukoliikenne',
                        propertyValue: '3' }
                    ]}
                ],
                readOnly: true,
                roadLinkId: 1140018963,
                validityDirection: 2,
                validityPeriod: 'current',
                wgslat: 60.2128746641816,
                wgslon: 24.7375812322645
            };
        }
    });
});
