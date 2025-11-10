module.exports = function(api) {
  const isTest = api.env('test');

  return {
    presets: [
      '@babel/preset-env',
      '@babel/preset-flow',
      '@babel/preset-react',
      ['@babel/preset-typescript', {
        isTSX: true,
        allExtensions: true,
        // Don't check .js files for TypeScript
        onlyRemoveTypeImports: true
      }],
    ],
    plugins: [
      ['@babel/plugin-proposal-decorators', { legacy: true }],
    ],
  };
};
