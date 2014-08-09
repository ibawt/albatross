$(function() {
    $('#search-form').on('submit', function(event) {
        return false;
    });
    $('#search-input').on('input', _.debounce(function(event) {
        if( !event.currentTarget.value ) {
            $('#search-results').empty();
            return false;
        }

        $.ajax("/shows/choose", {
            data: $('#search-form').serialize(),
            dataType: 'html',
            type: 'POST'
        }).done( function(data) {
            var $searchList = $('#search-results');
            var $data = $(data);
            $searchList.empty();
            $searchList.append($data);
        });
    }, 300));
});

function onBannerImageError(node) {
    var box = $(node).parent();
    box.hide();
    box.parent().find('p').show();
};

function addShowClicked(node) {
    var tvdb_id = node.getAttribute('data-href');
    $('#show').val( tvdb_id );
    $('#create-form').submit();
}
