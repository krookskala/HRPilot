// Shared test environment setup.
// TestBed init is still done per-file to avoid module isolation issues in Vitest v4.

const noop = () => {};

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  configurable: true,
  value: () => ({
    canvas: document.createElement('canvas'),
    fillRect: noop,
    clearRect: noop,
    getImageData: noop,
    putImageData: noop,
    createImageData: noop,
    setTransform: noop,
    drawImage: noop,
    save: noop,
    fillText: noop,
    restore: noop,
    beginPath: noop,
    moveTo: noop,
    lineTo: noop,
    closePath: noop,
    stroke: noop,
    translate: noop,
    scale: noop,
    rotate: noop,
    arc: noop,
    fill: noop,
    measureText: () => ({ width: 0 }),
    transform: noop,
    rect: noop,
    clip: noop
  })
});
