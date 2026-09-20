                    fetch(url, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(requestBody),
                        credentials: 'same-origin'
                    })
                        .then(function (response) {
                            if (response.ok) {
                                return response.json().then(function (data) {
                                    // If the fetch interceptor (taloms.js) already queued this
                                    // request offline, it returns a synthetic 202 {queued:true}.
                                    // Treat that as an offline-save success, not a real redirect.
                                    if (data && data.queued) {
                                        return showSavedOffline(submitBtn);
                                    }
                                    // Success - redirect to parcel detail
                                    var id = data.data && data.data.id ? data.data.id : '';
                                    window.location.href = '/parcels/' + id;
                                });
                            }
                            // IT IS AN HTTP ERROR
                            return response.json().catch(function() { return {}; }).then(function(errData) {
                                var msg = errData.message || ('Server error: ' + response.status);
                                submitBtn.innerHTML = originalText;
                                submitBtn.disabled = false;
                                if (typeof isSubmitting !== 'undefined') isSubmitting = false;
                                alert(msg);
                            });
                        })
                        .catch(function (err) {
                            console.warn('Online save failed, queuing for sync:', err.message);
                            // Queue the REST create in the outbox for later sync
                            if (window.Taloms && Taloms.Outbox) {
                                Taloms.Outbox.enqueue('POST', url, JSON.stringify(requestBody), { 'Content-Type': 'application/json' })
                                    .then(function () {
                                        showSavedOffline(submitBtn);
                                    })
                                    .catch(function () {
                                        submitBtn.innerHTML = originalText;
                                        submitBtn.disabled = false;
                                        if (typeof isSubmitting !== 'undefined') isSubmitting = false;
                                        alert('Failed to save offline. Please try again.');
                                    });
                            } else {
                                submitBtn.innerHTML = originalText;
                                submitBtn.disabled = false;
                                if (typeof isSubmitting !== 'undefined') isSubmitting = false;
                                alert('Cannot save offline. Please check your connection.');
                            }
                        });
