(() => {
    'use strict';

    const saleMatch = window.location.pathname.match(/^\/sales\/(\d+)$/);
    if (!saleMatch) {
        return;
    }

    const saleId = saleMatch[1];
    const simulatedForm = document.querySelector(
        `form[action="/sales/${saleId}/payments"]`);
    if (!simulatedForm) {
        return;
    }

    simulatedForm.querySelectorAll(
        'option[value="APPLE_PAY"], option[value="GOOGLE_PAY"]')
        .forEach(option => option.remove());

    fetch(`/api/payments/wallets/config/${saleId}`, {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
    })
        .then(response => response.ok ? response.json() : null)
        .then(config => {
            if (!config || !config.enabled) {
                return;
            }
            renderWalletPanel(config);
        })
        .catch(() => {
            // El checkout simulado continúa disponible si la configuración
            // de wallets no puede consultarse.
        });

    function renderWalletPanel(config) {
        const section = document.createElement('section');
        section.className = 'content-card mt-4 wallet-payment-panel';
        section.innerHTML = `
            <h2 class="h5 fw-bold mb-1">Pago con billetera digital</h2>
            <p class="text-secondary mb-3">
                Cobra de forma segura mediante Apple Pay o Google Pay procesado por AZUL.
            </p>
            <div class="d-flex flex-wrap gap-3" data-wallet-buttons></div>
            <p class="small text-secondary mt-3 mb-0">
                Eventix no almacena números de tarjeta ni tokens de la billetera.
            </p>`;
        simulatedForm.closest('section').after(section);
        const container = section.querySelector('[data-wallet-buttons]');

        if (config.googleGatewayMerchantId) {
            loadGooglePay(config, container);
        }
        if (config.applePayEnabled) {
            renderApplePay(config, container);
        }
    }

    function loadGooglePay(config, container) {
        const script = document.createElement('script');
        script.src = 'https://pay.google.com/gp/p/js/pay.js';
        script.async = true;
        script.onload = () => renderGooglePay(config, container);
        document.head.appendChild(script);
    }

    function renderGooglePay(config, container) {
        if (!window.google?.payments?.api) {
            return;
        }
        const client = new google.payments.api.PaymentsClient({
            environment: config.environment === 'PRODUCTION'
                ? 'PRODUCTION'
                : 'TEST'
        });
        const baseCard = {
            type: 'CARD',
            parameters: {
                allowedAuthMethods: ['PAN_ONLY', 'CRYPTOGRAM_3DS'],
                allowedCardNetworks: [
                    'AMEX', 'DISCOVER', 'JCB', 'MASTERCARD', 'VISA'
                ]
            }
        };
        const paymentMethod = {
            ...baseCard,
            tokenizationSpecification: {
                type: 'PAYMENT_GATEWAY',
                parameters: {
                    gateway: 'pagosazul',
                    gatewayMerchantId: config.googleGatewayMerchantId
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
                onClick: () => client.loadPaymentData({
                    apiVersion: 2,
                    apiVersionMinor: 0,
                    allowedPaymentMethods: [paymentMethod],
                    merchantInfo: {merchantName: 'Eventix'},
                    transactionInfo: {
                        totalPriceStatus: 'FINAL',
                        totalPrice: config.totalPrice,
                        currencyCode: config.currency,
                        countryCode: config.countryCode
                    }
                }).then(paymentData => {
                    const token = paymentData.paymentMethodData
                        ?.tokenizationData?.token;
                    if (token) {
                        submitWallet('GOOGLE_PAY', token);
                    }
                }).catch(error => {
                    if (error?.statusCode !== 'CANCELED') {
                        showError('No se pudo iniciar Google Pay.');
                    }
                })
            });
            container.appendChild(button);
        }).catch(() => {});
    }

    function renderApplePay(config, container) {
        if (!window.ApplePaySession || !ApplePaySession.canMakePayments()) {
            return;
        }
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'apple-pay-button';
        button.setAttribute('aria-label', 'Pagar con Apple Pay');
        button.addEventListener('click', () => startApplePay(config));
        container.appendChild(button);
    }

    function startApplePay(config) {
        const request = {
            countryCode: config.countryCode,
            currencyCode: config.currency,
            supportedNetworks: ['visa', 'masterCard', 'amex', 'discover'],
            merchantCapabilities: ['supports3DS'],
            total: {
                label: 'Eventix',
                amount: config.totalPrice,
                type: 'final'
            }
        };
        const session = new ApplePaySession(3, request);
        session.onvalidatemerchant = () => {
            fetch('/api/payments/wallets/apple/session', {
                credentials: 'same-origin',
                headers: {'Accept': 'application/json'}
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('merchant-session');
                    }
                    return response.json();
                })
                .then(merchantSession => {
                    session.completeMerchantValidation(merchantSession);
                })
                .catch(() => {
                    session.abort();
                    showError('No se pudo validar Apple Pay con AZUL.');
                });
        };
        session.onpaymentauthorized = event => {
            const token = JSON.stringify(event.payment.token.paymentData);
            session.completePayment(ApplePaySession.STATUS_SUCCESS);
            submitWallet('APPLE_PAY', token);
        };
        session.begin();
    }

    function submitWallet(provider, token) {
        const form = document.createElement('form');
        form.method = 'post';
        form.action = `/sales/${saleId}/wallet-payments`;
        append(form, 'provider', provider);
        append(form, 'walletToken', token);
        const csrf = simulatedForm.querySelector('input[name="_csrf"]');
        if (csrf) {
            append(form, csrf.name, csrf.value);
        }
        document.body.appendChild(form);
        form.submit();
    }

    function append(form, name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }

    function showError(message) {
        if (window.Swal) {
            Swal.fire({icon: 'error', title: 'Pago no disponible', text: message});
        }
    }
})();
