config.module.rules.push({
    test: /rust\.js$/,
    sideEffects: true
});
if (config.devServer) {
    config.devServer.headers = {
        "Document-Isolation-Policy": "isolate-and-credentialless"
    };
}