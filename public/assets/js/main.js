// Global state
var attachments_base_url = "https://nercacuk.sharepoint.com/sites/BASMagicTeam/felnneapitest2/";
var attachments = [];

// Request form setup
jQuery(function(jQuery){
  var now = new Date();
  var nextWeek = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 8);
  $("#request-form-need-by-date").val(nextWeek.toISOString().substring(0, 10));
});

// Request form submission
jQuery(function(jQuery){
  $("#request-form-submit").click(function() {
    // a hidden submit button is added and clicked to trigger the browsers native form validation
    $('<input type="submit">').hide().appendTo("#request-form").click().remove();
  })
});

// Request form handling
jQuery(function(jQuery){
  $("#request-form").submit(function(e) {
    e.preventDefault();
    $("#request-form-submit").toggleClass("bsk-disabled");
    $("#request-form-submit i").toggleClass("fa-envelope");
    $("#request-form-submit i").toggleClass("fa-spin");
    $("#request-form-submit i").toggleClass("fa-circle-notch");
    $("#request-form-submit span").text("Submitting request");

    var md = window.markdownit();
    md.set({gfm: true});

    var payload = {
      "summary": "Helpdesk request",
      "content": md.render($("#request-form #request-form-content").val()),
      "sender-name": $("#request-form #request-form-sender-name").val(),
      "sender-email": $("#request-form #request-form-sender-email").val(),
      "need-by-date": $("#request-form-need-by-date").val(),
      "attachments": attachments,
    };

    fetch("https://prod-135.westeurope.logic.azure.com:443/workflows/494981bee28549508574591f9e4b32f8/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=vYYX9CfzI9TKkD5BR2rr-8MpwGvj3E7rrzCE35Vu-HE", {
        method: "post",
        headers: new Headers({"content-type": "application/json;charset=UTF-8"}),
        body: JSON.stringify(payload)
    }).then(function (response) {
      if (response.ok) {
        $("#request-form-submit").toggleClass("bsk-btn-primary");
        $("#request-form-submit").toggleClass("bsk-btn-success");
        $("#request-form-submit i").toggleClass("fa-spin");
        $("#request-form-submit i").toggleClass("fa-circle-notch");
        $("#request-form-submit i").toggleClass("fa-check");
        $("#request-form-submit span").text("Request submitted");

        if($("#request-form-result").hasClass("bsk-hidden")){
          $("#request-form-result").removeClass("bsk-hidden");
        }
        if(!$("#request-form-result").hasClass("bsk-in")){
          $("#request-form-result").addClass("bsk-in");
        }
        if(!$("#request-form-result").hasClass("bsk-alert-success")){
          $("#request-form-result").addClass("bsk-alert-success");
        }
        $("#request-form-result").text("Request submitted successfully. You can now safely close this page.");
      } else {
        set_error_state();
      }
    }).catch(function (err) {
      set_error_state();
    });
  });
});

function set_error_state() {
  $("#request-form-submit").toggleClass("bsk-disabled");
  $("#request-form-submit").toggleClass("bsk-btn-primary");
  $("#request-form-submit").toggleClass("bsk-btn-default");
  $("#request-form-submit i").toggleClass("fa-spin");
  $("#request-form-submit i").toggleClass("fa-circle-notch");
  $("#request-form-submit i").toggleClass("fa-redo");
  $("#request-form-submit span").text("Retry submission");

  if($("#request-form-result").hasClass("bsk-hidden")){
    $("#request-form-result").removeClass("bsk-hidden");
  }
  if(!$("#request-form-result").hasClass("bsk-in")){
    $("#request-form-result").addClass("bsk-in");
  }
  if(!$("#request-form-result").hasClass("bsk-alert-danger")){
    $("#request-form-result").addClass("bsk-alert-danger");
  }
  $("#request-form-result").html('Sorry, something went wrong submitting your request. Please try again or email <a href="mailto:servicedesk@bas.ac.uk" class="bsk-alert-link">servicedesk@bas.ac.uk</a> if this problem persists.');
};

// File upload handling
jQuery(function(jQuery){
  Dropzone.autoDiscover = false;

  var fileUploadPreviewTemplate = jQuery('#file-upload .bsk-dropzone-file-list .bsk-dropzone-file-list-items').html();
  jQuery('#file-upload .bsk-dropzone-file-list .bsk-dropzone-file-list-items').empty();

  var fileUploadDropzone = new Dropzone('#file-upload', {
    url: 'https://prod-161.westeurope.logic.azure.com:443/workflows/554add980c5340c7870750e8bfe1290b/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=RG19x0n6F__TC2mhH1QJTlRJzghb7zd7kvP-BOC2_1g',
    previewsContainer: '#file-upload .bsk-dropzone-file-list-items',
    previewTemplate: fileUploadPreviewTemplate,
    createImageThumbnails: false,
    clickable: "#file-upload .bsk-target-inner"
  });

  fileUploadDropzone.on("addedfile", function(file) {
    jQuery('#' + fileUploadDropzone.element.id).addClass('bsk-dropzone-started');
  });
  fileUploadDropzone.on("reset", function(file) {
    jQuery('#' + fileUploadDropzone.element.id).removeClass('bsk-dropzone-started');
  });
  fileUploadDropzone.on("processing", function(file) {
    jQuery(file.previewElement).find('[data-bsk-dz-status]').text('Pending...');
  });
  fileUploadDropzone.on("uploadprogress", function(file, progress) {
    jQuery(file.previewElement).find('[data-bsk-dz-status]').text('Uploading (' + Math.round(progress) + '%)');
  });
  fileUploadDropzone.on("success", function(file, message) {
    attachments.push({
      'name': file.name,
      'url': attachments_base_url + file.name
    });
    jQuery(file.previewElement).find('[data-bsk-dz-status]').addClass('bsk-text-success').text('Uploaded');
  });
  fileUploadDropzone.on("error", function(file, message, xhr) {
    jQuery('#' + fileUploadDropzone.element.id).addClass('bsk-dropzone-errors');
    jQuery(file.previewElement).find('[data-bsk-dz-status]').addClass('bsk-text-danger').text('Upload Error');
    jQuery(file.previewElement).find('[data-dz-name]').addClass('bsk-text-danger');

    var errorMessage = '[' + xhr.status + '] ' + xhr.statusText;

    errorMessage = '<li data-bsk-dz-file-id="' + base64Encode(file.name) + '"><strong>' + file.name + '</strong> - ' + errorMessage + '</li>';
    jQuery('#' + fileUploadDropzone.element.id).find('.bsk-dropzone-errors-container ul').append(errorMessage);
  });
  fileUploadDropzone.on("removedfile", function(file) {
    jQuery('#' + fileUploadDropzone.element.id).find('.bsk-dropzone-errors-container ul li[data-bsk-dz-file-id="' + base64Encode(file.name) + '"]').remove();

    // Check if errors container is now empty
    if (jQuery('#' + fileUploadDropzone.element.id).find('.bsk-dropzone-errors-container ul li').length < 1) {
      jQuery('#' + fileUploadDropzone.element.id).removeClass('bsk-dropzone-errors');
    }
  });
});
