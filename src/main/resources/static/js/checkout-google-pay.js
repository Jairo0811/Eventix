(() => {
    'use strict';

    const config = document.querySelector('[data-google-pay-checkout]');
    const form = document.querySelector('[data-customer-checkout-form]');
    if (!config || !form || config.dataset.enabled !== 'true') {
        return;
    }

    const buttonContainer = config.querySelector('[data-google-pay-button]');
    const providerInput = form.querySelector('[name="provider"]');
    const walletTokenInput = form.querySelector('[name="walletToken"]');
    const csrfInput = form.querySelector('input[name="_csrf"]');

    if (!buttonContainer || !providerInput || !walletTokenInput) {
        return;
    }

    loadGooglePaySdk()
        .then(renderGooglePay)
        .catch(() => showError(
            'Google Pay no está disponible en este momento.'));

    function loadGooglePaySdk() {
        if (window.google?.payments?.api) {
            return Promise.resolve();
        }
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = 'https://pay.google.com/gp/p/js/pay.js';
            script.async = true;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    function renderGooglePay() {
        const client = new google.payments.api.PaymentsClient({
            environment: config.dataset.environment === 'PRODUCTION'
                ? 'PRODUCTION'
                : 'TEST'
        });
        const baseCard = {
            type: 'CARD',
            parameters: {
                allowedAuthMethods: ['PAN_ONLY', 'CRYPTOGRAM_3DS'],
                allowedCardNetworks: [
                    'AMEX',
                    'DISCOVER',
                    'JCB',
                    'MASTERCARD',
                    'VISA'
                ]
            }
        };
        const paymentMethod = {
            ...baseCard,
            tokenizationSpecification: {
                type: 'PAYMENT_GATEWAY',
                parameters: {
                    gateway: 'pagosazul',
                    gatewayMerchantId: config.dataset.gatewayMerchantId
                }
            }
        };

        client.isReadyToPay({
            apiVersion: 2,
            apiVersionMinor: 0,
            allowedPaymentMethods: [baseCard]
        }).then(result => {
            if (!result.result) {
                return;
            }
            const button = client.createButton({
                buttonType: 'buy',
                onClick: () => startPayment(client, paymentMethod)
            });
            buttonContainer.replaceChildren(button);
        }).catch(() => {});
    }

    async function startPayment(client, paymentMethod) {
        if (!form.reportValidity()) {
            return;
        }

        try {
            const quote = await requestQuote();
            // Con asientos reservados la cantidad cotizada es la de la
            // retención. Se refleja en el formulario para que el comprador vea
            // el mismo número que respalda el importe de la hoja de pago.
            const quantityInput = form.querySelector('[name="quantity"]');
            if (quantityInput && Number(quote.quantity) > 0
                    && Number(quote.quantity) !== Number(quantityInput.value)) {
                quantityInput.value = quote.quantity;
            }
            if (Number(quote.total) <= 0) {
                walletTokenInput.value = '';
                form.requestSubmit();
                return;
            }

            const merchantInfo = {
                merchantName: config.dataset.merchantName || 'Eventix'
            };
            if (config.dataset.environment === 'PRODUCTION') {
                merchantInfo.merchantId = config.dataset.merchantId;
            }

            const paymentData = await client.loadPaymentData({
                apiVersion: 2,
                apiVersionMinor: 0,
                allowedPaymentMethods: [paymentMethod],
                merchantInfo,
                transactionInfo: {
                    totalPriceStatus: 'FINAL',
                    totalPrice: Number(quote.total).toFixed(2),
                    currencyCode: quote.currency,
                    countryCode: config.dataset.countryCode || 'DO'
                }
            });
            const token = paymentData.paymentMethodData
                ?.tokenizationData?.token;
            if (!token) {
                throw new Error('missing-token');
            }

            providerInput.value = 'GOOGLE_PAY';
            walletTokenInput.value = token;
            form.submit();
        } catch (error) {
            if (error?.statusCode !== 'CANCELED') {
                showError(error?.message === 'quote-rejected'
                    ? 'No fue posible validar el total de la compra.'
                    : 'No se pudo completar el pago con Google Pay.');
            }
        }
    }

    async function requestQuote() {
        const ticketType = form.querySelector(
            'input[name="ticketTypeId"]:checked');
        if (!ticketType) {
            throw new Error('quote-rejected');
        }

        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        };
        if (csrfInput) {
            headers['X-CSRF-TOKEN'] = csrfInput.value;
        }

        const response = await fetch(`${form.action}/quote`, {
            method: 'POST',
            credentials: 'same-origin',
            headers,
            body: JSON.stringify({
                ticketTypeId: Number(ticketType.value),
                quantity: Number(form.querySelector('[name="quantity"]').value),
                couponCode: form.querySelector('[name="couponCode"]').value,
                // Con asientos reservados manda la retención, no este campo:
                // sin el token la cotización y el cobro podrían no coincidir.
                holdToken: form.querySelector('[name="holdToken"]')?.value || null
            })
        });
        if (!response.ok) {
            throw new Error('quote-rejected');
        }
        return response.json();
    }

    function showError(message) {
        if (window.Swal) {
            Swal.fire({
                icon: 'error',
                title: 'Google Pay no disponible',
                text: message
            });
            return;
        }
        window.alert(message);
    }
})();
