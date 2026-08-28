import { execFileSync } from 'node:child_process'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(scriptDirectory, '..')
const defaultAssetDirectory = path.join(frontendRoot, 'src', 'assets', 'home', 'optimized')

export const alphaThreshold = 250
export const assetRequirements = {
  'xiaozhi-robot-transparent.webp': { minHighAlphaPixels: 90000, maxHighAlphaComponents: 1 },
  'holographic-resume-transparent.webp': { minHighAlphaPixels: 120000, maxHighAlphaComponents: 1 },
  'career-target-transparent.webp': { minHighAlphaPixels: 100000, maxHighAlphaComponents: 1 },
  'course-cube-transparent.webp': { minHighAlphaPixels: 60000, maxHighAlphaComponents: 40 },
}

const assetNames = Object.keys(assetRequirements)

function summarizeColor(r, g, b) {
  const maxChannel = Math.max(r, g, b)
  const minChannel = Math.min(r, g, b)
  return {
    brightness: (r + g + b) / 3,
    chroma: maxChannel - minChannel,
  }
}

function squaredColorDistance(pixel, center) {
  const dr = pixel[0] - center[0]
  const dg = pixel[1] - center[1]
  const db = pixel[2] - center[2]
  return dr * dr + dg * dg + db * db
}

function collectBorderPixels(image) {
  const pixels = []
  const seen = new Set()
  const { data, width, height } = image

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      if (x !== 0 && y !== 0 && x !== width - 1 && y !== height - 1) {
        continue
      }

      const index = y * width + x
      if (seen.has(index)) continue
      seen.add(index)
      const offset = index * 4
      pixels.push([data[offset], data[offset + 1], data[offset + 2]])
    }
  }

  return pixels
}

function clusterBorderPalette(borderPixels, clusterCount = 4, iterations = 8) {
  if (borderPixels.length === 0) return []

  const sorted = [...borderPixels].sort((left, right) => {
    const leftLightness = left[0] + left[1] + left[2]
    const rightLightness = right[0] + right[1] + right[2]
    if (leftLightness !== rightLightness) return leftLightness - rightLightness
    if (left[0] !== right[0]) return left[0] - right[0]
    if (left[1] !== right[1]) return left[1] - right[1]
    return left[2] - right[2]
  })

  const initialCenters = []
  for (let index = 0; index < Math.min(clusterCount, sorted.length); index += 1) {
    const position = Math.round((index * (sorted.length - 1)) / Math.max(1, clusterCount - 1))
    initialCenters.push([...sorted[position]])
  }

  const centers = initialCenters

  for (let iteration = 0; iteration < iterations; iteration += 1) {
    const sums = centers.map(() => [0, 0, 0, 0])

    for (const pixel of borderPixels) {
      let nearestCenter = 0
      let nearestDistance = Number.POSITIVE_INFINITY

      for (let centerIndex = 0; centerIndex < centers.length; centerIndex += 1) {
        const distance = squaredColorDistance(pixel, centers[centerIndex])
        if (distance < nearestDistance) {
          nearestDistance = distance
          nearestCenter = centerIndex
        }
      }

      sums[nearestCenter][0] += pixel[0]
      sums[nearestCenter][1] += pixel[1]
      sums[nearestCenter][2] += pixel[2]
      sums[nearestCenter][3] += 1
    }

    for (let centerIndex = 0; centerIndex < centers.length; centerIndex += 1) {
      const [red, green, blue, count] = sums[centerIndex]
      if (count === 0) continue
      centers[centerIndex] = [
        Math.round(red / count),
        Math.round(green / count),
        Math.round(blue / count),
      ]
    }
  }

  const counts = centers.map(() => 0)
  for (const pixel of borderPixels) {
    let nearestCenter = 0
    let nearestDistance = Number.POSITIVE_INFINITY

    for (let centerIndex = 0; centerIndex < centers.length; centerIndex += 1) {
      const distance = squaredColorDistance(pixel, centers[centerIndex])
      if (distance < nearestDistance) {
        nearestDistance = distance
        nearestCenter = centerIndex
      }
    }

    counts[nearestCenter] += 1
  }

  return centers.map((center, index) => ({ center, count: counts[index] }))
}

function buildBackgroundPalette(image) {
  const borderPixels = collectBorderPixels(image)
  const clusters = clusterBorderPalette(borderPixels)
  const minimumClusterSize = Math.max(24, Math.round(borderPixels.length * 0.05))

  return clusters
    .filter(({ center, count }) => {
      const { brightness, chroma } = summarizeColor(center[0], center[1], center[2])
      return count >= minimumClusterSize && brightness >= 165 && chroma <= 30
    })
    .map(({ center }) => center)
}

function buildMask(image, predicate) {
  const mask = new Uint8Array(image.width * image.height)
  for (let index = 0; index < mask.length; index += 1) {
    mask[index] = predicate(index) ? 1 : 0
  }
  return mask
}

function inspectComponents(mask, width, height) {
  const visited = new Uint8Array(mask.length)
  const components = []

  for (let start = 0; start < mask.length; start += 1) {
    if (!mask[start] || visited[start]) continue

    const queue = [start]
    visited[start] = 1
    let cursor = 0
    let minX = width
    let minY = height
    let maxX = 0
    let maxY = 0
    let area = 0
    let touchesEdge = false
    const pixels = []

    while (cursor < queue.length) {
      const index = queue[cursor]
      cursor += 1
      const x = index % width
      const y = Math.floor(index / width)

      pixels.push(index)
      area += 1
      minX = Math.min(minX, x)
      minY = Math.min(minY, y)
      maxX = Math.max(maxX, x)
      maxY = Math.max(maxY, y)
      touchesEdge ||= x === 0 || y === 0 || x === width - 1 || y === height - 1

      if (x > 0) {
        const left = index - 1
        if (mask[left] && !visited[left]) {
          visited[left] = 1
          queue.push(left)
        }
      }
      if (x + 1 < width) {
        const right = index + 1
        if (mask[right] && !visited[right]) {
          visited[right] = 1
          queue.push(right)
        }
      }
      if (y > 0) {
        const up = index - width
        if (mask[up] && !visited[up]) {
          visited[up] = 1
          queue.push(up)
        }
      }
      if (y + 1 < height) {
        const down = index + width
        if (mask[down] && !visited[down]) {
          visited[down] = 1
          queue.push(down)
        }
      }
    }

    const boxWidth = maxX - minX + 1
    const boxHeight = maxY - minY + 1
    components.push({
      area,
      boxArea: boxWidth * boxHeight,
      fillRatio: area / (boxWidth * boxHeight),
      touchesEdge,
      pixels,
    })
  }

  return components
}

function isNearBackgroundPalette(pixel, backgroundPalette) {
  return backgroundPalette.some((center) => squaredColorDistance(pixel, center) <= 3600)
}

function isHiddenColorMatte(data, index) {
  const offset = index * 4
  const alpha = data[offset + 3]
  const brightestChannel = Math.max(data[offset], data[offset + 1], data[offset + 2])
  return alpha <= 8 && brightestChannel >= 24
}

function buildFlatHiddenMatteMask(image, hiddenMatteMask) {
  const { data, width, height } = image
  const mask = new Uint8Array(hiddenMatteMask.length)

  for (let index = 0; index < hiddenMatteMask.length; index += 1) {
    if (!hiddenMatteMask[index]) continue

    const x = index % width
    const y = Math.floor(index / width)
    const offset = index * 4
    let matchingNeighbors = 0

    for (const neighbor of [
      x > 0 ? index - 1 : -1,
      x + 1 < width ? index + 1 : -1,
      y > 0 ? index - width : -1,
      y + 1 < height ? index + width : -1,
    ]) {
      if (neighbor < 0 || !hiddenMatteMask[neighbor]) continue
      const neighborOffset = neighbor * 4
      const red = data[offset] - data[neighborOffset]
      const green = data[offset + 1] - data[neighborOffset + 1]
      const blue = data[offset + 2] - data[neighborOffset + 2]

      if (red * red + green * green + blue * blue <= 3 * 48 * 48) {
        matchingNeighbors += 1
      }
    }

    mask[index] = matchingNeighbors >= 2 ? 1 : 0
  }

  return mask
}

function inspectMatteBands(mask, width, height) {
  let longestHorizontalMatteBand = 0
  let longestVerticalMatteBand = 0

  for (let y = 0; y < height; y += 1) {
    let run = 0
    for (let x = 0; x < width; x += 1) {
      run = mask[y * width + x] ? run + 1 : 0
      longestHorizontalMatteBand = Math.max(longestHorizontalMatteBand, run)
    }
  }

  for (let x = 0; x < width; x += 1) {
    let run = 0
    for (let y = 0; y < height; y += 1) {
      run = mask[y * width + x] ? run + 1 : 0
      longestVerticalMatteBand = Math.max(longestVerticalMatteBand, run)
    }
  }

  return { longestHorizontalMatteBand, longestVerticalMatteBand }
}

export function inspectTransparentPixels(image, requirements = {}) {
  const { data, width, height } = image
  const totalPixels = width * height
  const backgroundPalette = buildBackgroundPalette(image)
  const effectiveRequirements = {
    minHighAlphaPixels: 0,
    maxHighAlphaComponents: Number.POSITIVE_INFINITY,
    minHighAlphaComponentPixels: 8,
    maxEdgeMattePixels: 0,
    maxRectangularMattePixels: Math.max(64, Math.round(totalPixels * 0.004)),
    maxHiddenMattePixels: Math.max(64, Math.round(totalPixels * 0.004)),
    maxLowVarianceMatteRectanglePixels: Math.max(128, Math.round(totalPixels * 0.002)),
    maxMatteBandLength: Math.max(48, Math.round(Math.min(width, height) * 0.12)),
    ...requirements,
  }

  const highAlphaMask = buildMask(image, (index) => data[index * 4 + 3] >= alphaThreshold)
  const significantHighAlphaComponents = inspectComponents(highAlphaMask, width, height).filter(
    (component) => component.area >= effectiveRequirements.minHighAlphaComponentPixels,
  )
  const highAlphaPixels = highAlphaMask.reduce((count, pixel) => count + pixel, 0)

  const matteMask = buildMask(image, (index) => {
    const offset = index * 4
    const red = data[offset]
    const green = data[offset + 1]
    const blue = data[offset + 2]
    const alpha = data[offset + 3]
    const { brightness, chroma } = summarizeColor(red, green, blue)
    const nearBackgroundPalette = isNearBackgroundPalette([red, green, blue], backgroundPalette)
    const brightNeutral = brightness >= 175 && chroma <= 28
    const transparentWithHiddenMatte = alpha <= 8 && (brightness >= 160 || nearBackgroundPalette)
    const opaqueMatte = alpha > 8 && (brightNeutral || nearBackgroundPalette)
    return transparentWithHiddenMatte || opaqueMatte
  })
  const matteComponents = inspectComponents(matteMask, width, height)
  const hiddenMatteMask = buildMask(image, (index) => isHiddenColorMatte(data, index))
  const hiddenMatteComponents = inspectComponents(hiddenMatteMask, width, height)
  const flatHiddenMatteMask = buildFlatHiddenMatteMask(image, hiddenMatteMask)
  const flatHiddenMatteComponents = inspectComponents(flatHiddenMatteMask, width, height)
  const { longestHorizontalMatteBand, longestVerticalMatteBand } = inspectMatteBands(
    hiddenMatteMask,
    width,
    height,
  )

  let edgeMattePixels = 0
  let largestRectangularMattePixels = 0
  let largestHiddenMattePixels = 0
  let largestEdgeBackgroundPixels = 0
  const largestHiddenColorMattePixels = hiddenMatteComponents.reduce(
    (largest, component) => Math.max(largest, component.area),
    0,
  )
  const largestLowVarianceMatteRectanglePixels = flatHiddenMatteComponents.reduce(
    (largest, component) => {
      if (component.fillRatio < 0.2) return largest
      return Math.max(largest, component.area)
    },
    0,
  )

  for (const component of matteComponents) {
    let highAlphaArea = 0
    let hiddenArea = 0

    for (const pixelIndex of component.pixels) {
      const alpha = data[pixelIndex * 4 + 3]
      if (alpha >= alphaThreshold) highAlphaArea += 1
      if (alpha <= 8) hiddenArea += 1

      const x = pixelIndex % width
      const y = Math.floor(pixelIndex / width)
      if (
        (x === 0 || y === 0 || x === width - 1 || y === height - 1) &&
        alpha >= alphaThreshold
      ) {
        edgeMattePixels += 1
      }
    }

    if (component.fillRatio >= 0.72) {
      largestRectangularMattePixels = Math.max(largestRectangularMattePixels, component.area)
      if (hiddenArea / component.area >= 0.85) {
        largestHiddenMattePixels = Math.max(largestHiddenMattePixels, component.area)
      }
    }

    if (component.touchesEdge && highAlphaArea / component.area >= 0.6) {
      largestEdgeBackgroundPixels = Math.max(largestEdgeBackgroundPixels, component.area)
    }
  }

  const reasons = []
  if (highAlphaPixels < effectiveRequirements.minHighAlphaPixels) {
    reasons.push('subject coverage below minimum')
  }
  if (significantHighAlphaComponents.length > effectiveRequirements.maxHighAlphaComponents) {
    reasons.push('subject fragmentation above limit')
  }
  if (edgeMattePixels > effectiveRequirements.maxEdgeMattePixels) {
    reasons.push('edge matte pixels detected')
  }
  if (largestRectangularMattePixels > effectiveRequirements.maxRectangularMattePixels) {
    reasons.push('large rectangular matte detected')
  }
  if (largestHiddenMattePixels > effectiveRequirements.maxHiddenMattePixels) {
    reasons.push('hidden rectangular matte detected')
  }
  if (largestHiddenColorMattePixels > effectiveRequirements.maxHiddenMattePixels) {
    reasons.push('hidden colored matte detected')
  }
  if (
    largestLowVarianceMatteRectanglePixels >
    effectiveRequirements.maxLowVarianceMatteRectanglePixels
  ) {
    reasons.push('large low-variance rectangular matte detected')
  }
  if (
    longestHorizontalMatteBand > effectiveRequirements.maxMatteBandLength ||
    longestVerticalMatteBand > effectiveRequirements.maxMatteBandLength
  ) {
    reasons.push('long horizontal or vertical matte band detected')
  }

  return {
    passed: reasons.length === 0,
    reasons,
    width,
    height,
    totalPixels,
    backgroundPalette,
    highAlphaPixels,
    highAlphaComponents: significantHighAlphaComponents.length,
    edgeMattePixels,
    largestRectangularMattePixels,
    largestHiddenMattePixels,
    largestEdgeBackgroundPixels,
    largestHiddenColorMattePixels,
    largestLowVarianceMatteRectanglePixels,
    longestHorizontalMatteBand,
    longestVerticalMatteBand,
  }
}

function decodeImageData(assetPath) {
  if (process.platform !== 'win32') {
    throw new Error('The transparent asset verifier requires Windows WIC WebP decoding.')
  }

  const escapedAssetPath = path.resolve(assetPath).replace(/'/g, "''")
  const command = `$ProgressPreference = 'SilentlyContinue'
Add-Type -AssemblyName PresentationCore
$uri = [Uri]::new('${escapedAssetPath}')
$decoder = [System.Windows.Media.Imaging.BitmapDecoder]::Create(
  $uri,
  [System.Windows.Media.Imaging.BitmapCreateOptions]::PreservePixelFormat,
  [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
)
$bitmap = [System.Windows.Media.Imaging.FormatConvertedBitmap]::new(
  $decoder.Frames[0],
  [System.Windows.Media.PixelFormats]::Bgra32,
  $null,
  0
)
$stride = $bitmap.PixelWidth * 4
$pixels = [byte[]]::new($stride * $bitmap.PixelHeight)
$bitmap.CopyPixels($pixels, $stride, 0)
[Console]::Out.Write((@{
  width = $bitmap.PixelWidth
  height = $bitmap.PixelHeight
  data = [Convert]::ToBase64String($pixels)
} | ConvertTo-Json -Compress))`
  const encodedCommand = Buffer.from(command, 'utf16le').toString('base64')
  const decoded = JSON.parse(
    execFileSync(
      'powershell.exe',
      ['-NoProfile', '-NonInteractive', '-EncodedCommand', encodedCommand],
      { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] },
    ),
  )
  const bgra = Buffer.from(decoded.data, 'base64')
  const data = new Uint8ClampedArray(bgra.length)

  for (let offset = 0; offset < bgra.length; offset += 4) {
    data[offset] = bgra[offset + 2]
    data[offset + 1] = bgra[offset + 1]
    data[offset + 2] = bgra[offset]
    data[offset + 3] = bgra[offset + 3]
  }

  return { data, width: decoded.width, height: decoded.height }
}

async function runCli() {
  const assetDirectory = process.argv[2]
    ? path.resolve(process.argv[2])
    : defaultAssetDirectory
  let failed = false

  for (const assetName of assetNames) {
    const requirements = assetRequirements[assetName]
    const image = decodeImageData(path.join(assetDirectory, assetName))
    const result = inspectTransparentPixels(image, requirements)

    console.log(
      `${result.passed ? 'PASS' : 'FAIL'} ${assetName}: ${result.width}x${result.height}, ` +
        `high alpha ${result.highAlphaPixels}/${result.totalPixels}, ` +
        `${result.highAlphaComponents} component(s), edge matte ${result.edgeMattePixels}, ` +
        `largest matte ${result.largestRectangularMattePixels}, ` +
        `hidden matte ${result.largestHiddenMattePixels}, ` +
        `hidden color ${result.largestHiddenColorMattePixels}, ` +
        `flat rectangle ${result.largestLowVarianceMatteRectanglePixels}, ` +
        `bands h${result.longestHorizontalMatteBand}/v${result.longestVerticalMatteBand}, ` +
        `edge background ${result.largestEdgeBackgroundPixels}`,
    )
    failed ||= !result.passed
  }

  if (failed) {
    console.error(
      `Transparent asset verification failed (alpha >= ${alphaThreshold}; ` +
        'each asset must preserve subject coverage, avoid excessive foreground fragmentation, ' +
        'keep the canvas edge free of matte residue, and avoid large rectangular matte regions).',
    )
    process.exitCode = 1
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await runCli()
}
