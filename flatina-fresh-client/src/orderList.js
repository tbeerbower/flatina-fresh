import { formatAmount } from './format'

/** True when the text looks like a link rather than an item number. */
export function isLink(text) {
  return /^https?:\/\//i.test(text || '')
}

/** Recipe amounts as one string: "3 tbsp + 2 cloves". */
export function amountText(item) {
  return item.amounts.map((a) => formatAmount(a.quantity, a.unit)).join(' + ')
}

/** "Kikkoman Soy Sauce", or the ingredient name when only part of the product is saved. */
export function productName(item) {
  const p = item.product
  if (!p || (!p.brand && !p.productName)) return item.name
  return [p.brand, p.productName || item.name].filter(Boolean).join(' ')
}

const MEASURES = ['oz', 'lb', 'g', 'fl oz', 'ml', 'cup', 'tbsp', 'tsp']

/** "10 fl oz bottle", "head (10 cloves)", "1 lb", or '' when no package is saved. */
export function packageText(product) {
  if (!product) return ''
  const label = product.packageLabel && product.packageLabel !== 'each' ? product.packageLabel : ''
  const size = product.packageSize && product.packageUnit ? formatAmount(product.packageSize, product.packageUnit) : ''
  if (size && label && !MEASURES.includes(product.packageUnit)) return `${label} (${size})`
  return [size, label].filter(Boolean).join(' ')
}

/** What to buy: "2 × 10 fl oz bottle" when packages are known, otherwise the recipe amounts. */
export function buyText(item) {
  const pkg = packageText(item.product)
  if (item.packages) return pkg ? `${item.packages} × ${pkg}` : `${item.packages}`
  return amountText(item)
}

/** One line of the copied order: "Kikkoman Soy Sauce, 1 × 10 fl oz bottle (#12345)". */
export function orderLine(item) {
  const buy = buyText(item)
  let line = buy ? `${productName(item)}, ${buy}` : productName(item)
  const store = item.product && item.product.storeItem
  if (store) line += isLink(store) ? `\n    ${store}` : ` (#${store.replace(/^#/, '')})`
  return line
}

/** Plain-text order for pasting into a store's search, a note, or a message, grouped by aisle. */
export function orderText(title, items) {
  const lines = [title]
  let aisle = null
  for (const item of items) {
    if (item.aisle !== aisle) {
      aisle = item.aisle
      lines.push('', aisle === 'Pantry' ? 'Pantry' : aisle)
    }
    lines.push(`- ${orderLine(item)}`)
  }
  return lines.join('\n')
}
