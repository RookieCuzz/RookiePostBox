const assert = require('node:assert/strict')
const { once } = require('node:events')
const mineflayer = require('mineflayer')

const SERVER_HOST = process.env.MC_HOST || '127.0.0.1'
const SERVER_PORT = Number(process.env.MC_PORT || 25565)
const MINECRAFT_VERSION = process.env.MC_VERSION || '1.21.4'
const BOT_USERNAME = process.env.MC_USERNAME || 'RPostBoxTest'
const RECIPIENT_A_USERNAME = process.env.MC_RECIPIENT_A || 'RPostBoxA'
const RECIPIENT_B_USERNAME = process.env.MC_RECIPIENT_B || 'RPostBoxB'
const TEST_ITEM = process.env.TEST_ITEM || 'minecraft:diamond'
const BULK_TEST_ITEM = process.env.BULK_TEST_ITEM || 'minecraft:emerald'
const PAGE_TEST_ITEM = process.env.PAGE_TEST_ITEM || 'minecraft:copper_ingot'
const RAPID_TEST_ITEM = process.env.RAPID_TEST_ITEM || 'minecraft:gold_ingot'
const DISCONNECT_TEST_ITEM = process.env.DISCONNECT_TEST_ITEM || 'minecraft:iron_ingot'
const BOUNDARY_TEST_ITEM = process.env.BOUNDARY_TEST_ITEM || 'minecraft:lapis_lazuli'
const EMPTY_TEST_ITEM = process.env.EMPTY_TEST_ITEM || 'minecraft:redstone'
const NOTIFY_TEST_ITEM = process.env.NOTIFY_TEST_ITEM || 'minecraft:quartz'
const DETAIL_TEST_ITEM = process.env.DETAIL_TEST_ITEM || 'minecraft:amethyst_shard'
const TEST_MESSAGE = `mineflayer-${Date.now()}`
const BULK_TEST_MESSAGE = `mineflayer-bulk-${Date.now()}`
const RUN_SUFFIX = String(Date.now()).slice(-4)
const PAGE_RECIPIENT_USERNAME = process.env.MC_PAGE_RECIPIENT || `RPBPage${RUN_SUFFIX}`
const RAPID_RECIPIENT_USERNAME = process.env.MC_RAPID_RECIPIENT || `RPBFast${RUN_SUFFIX}`
const DISCONNECT_RECIPIENT_USERNAME = process.env.MC_DISCONNECT_RECIPIENT || `RPBDrop${RUN_SUFFIX}`
const BOUNDARY_RECIPIENT_USERNAME = process.env.MC_BOUNDARY_RECIPIENT || `RPBBound${RUN_SUFFIX}`
const EMPTY_RECIPIENT_USERNAME = process.env.MC_EMPTY_RECIPIENT || `RPBEmpty${RUN_SUFFIX}`
const NOTIFY_RECIPIENT_USERNAME = process.env.MC_NOTIFY_RECIPIENT || `RPBNote${RUN_SUFFIX}`
const DETAIL_RECIPIENT_USERNAME = process.env.MC_DETAIL_RECIPIENT || `RPBView${RUN_SUFFIX}`
const MAILBOX_PACKAGE_SLOTS = [
  19, 20, 21, 22, 23, 24, 25,
  28, 29, 30, 31, 32, 33, 34,
  37, 38, 39, 40, 41, 42, 43
]
const MAILBOX_RESERVED_EMPTY_SLOTS = [18, 26, 36, 44]
const DETAIL_ATTACHMENT_SLOTS = [38, 39, 40, 41, 47, 48, 49, 50]
const CUSTOM_MODEL_DATA = {
  previous: 21001,
  next: 21002,
  mailItem: 21003
}

function createBot(username = BOT_USERNAME) {
  const bot = mineflayer.createBot({
    host: SERVER_HOST,
    port: SERVER_PORT,
    username,
    version: MINECRAFT_VERSION,
    auth: process.env.MC_AUTH || 'offline'
  })

  bot.on('error', (error) => {
    bot.lastError = error
  })

  bot.on('kicked', (reason) => {
    bot.lastKickReason = reason
  })

  bot.on('resourcePack', () => {
    bot.acceptResourcePack()
  })

  bot.observedMessages = []
  bot.on('messagestr', (message) => {
    bot.observedMessages.push(message)
  })

  return bot
}

async function waitForTicksOrThrow(bot, ticks) {
  for (let tick = 0; tick < ticks; tick += 1) {
    if (bot.lastError) throw bot.lastError
    if (bot.lastKickReason) throw new Error(`Bot was kicked: ${bot.lastKickReason}`)
    await new Promise((resolve) => setTimeout(resolve, 50))
  }
}

async function waitForMessage(bot, matcher, maxTicks = 100, startIndex = bot.observedMessages.length) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    const message = bot.observedMessages.slice(startIndex).find((candidate) => matcher.test(candidate))
    if (message) return message
    await waitForTicksOrThrow(bot, 1)
  }

  throw new Error(`Timed out waiting for chat message ${matcher}. Seen: ${bot.observedMessages.slice(startIndex).join(' | ')}`)
}

async function sendCommandAndWaitForMessage(bot, command, matcher, maxTicks = 100) {
  const startIndex = bot.observedMessages.length
  bot.chat(command)
  return waitForMessage(bot, matcher, maxTicks, startIndex)
}

function countInventoryItems(bot, itemName) {
  return bot.inventory.items().reduce((total, item) => {
    return item.name === itemName ? total + item.count : total
  }, 0)
}

async function waitForInventoryCountAtLeast(bot, itemName, minimumCount, maxTicks = 100) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    const currentCount = countInventoryItems(bot, itemName)
    if (currentCount >= minimumCount) return currentCount
    await waitForTicksOrThrow(bot, 1)
  }

  throw new Error(`Timed out waiting for ${itemName} count >= ${minimumCount}; current=${countInventoryItems(bot, itemName)}`)
}

async function waitForInventoryCount(bot, itemName, expectedCount, maxTicks = 100) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    const currentCount = countInventoryItems(bot, itemName)
    if (currentCount === expectedCount) return currentCount
    await waitForTicksOrThrow(bot, 1)
  }

  throw new Error(`Timed out waiting for ${itemName} count === ${expectedCount}; current=${countInventoryItems(bot, itemName)}`)
}

async function waitForWindowSlotEmpty(bot, slot, maxTicks = 100) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    if (bot.currentWindow && !bot.currentWindow.slots[slot]) return
    await waitForTicksOrThrow(bot, 1)
  }
  throw new Error(`Timed out waiting for window slot ${slot} to become empty; slots=${JSON.stringify(summarizeWindowSlots(bot.currentWindow), null, 2)}`)
}

function getWindowItemDisplayName(item) {
  const componentName = item?.components?.find?.((component) => {
    return component.type === 'minecraft:custom_name' || component.type === 'custom_name'
  })
  if (componentName?.data) {
    if (typeof componentName.data === 'string') return componentName.data
    if (typeof componentName.data === 'object') {
      return componentName.data.text || JSON.stringify(componentName.data)
    }
  }

  const customName = item?.customName
  if (customName) {
    if (typeof customName === 'string') return customName
    return customName.text || JSON.stringify(customName)
  }

  if (!item || !item.nbt) return ''
  const display = item.nbt.value?.display?.value
  const name = display?.Name?.value
  if (!name) return ''

  try {
    const parsed = JSON.parse(name)
    return parsed.text || parsed.extra?.map((part) => part.text || '').join('') || name
  } catch {
    return String(name)
  }
}

function componentText(value) {
  if (!value) return ''
  if (typeof value === 'string') {
    try {
      return componentText(JSON.parse(value))
    } catch {
      return value
    }
  }
  if (Array.isArray(value)) {
    return value.map(componentText).join('')
  }
  if (typeof value === 'object') {
    const text = value.text || value.translate || ''
    const extra = value.extra ? componentText(value.extra) : ''
    return `${text}${extra}`
  }
  return String(value)
}

function getWindowItemLoreText(item) {
  if (!item) return ''
  const candidates = []
  const componentLore = item.components?.find?.((component) => {
    return component.type === 'minecraft:lore' || component.type === 'lore' || component.type === 'minecraft:item_lore' || component.type === 'item_lore'
  })
  if (componentLore?.data) {
    candidates.push(componentText(componentLore.data))
  }

  const lore = item.nbt?.value?.display?.value?.Lore?.value?.value
  if (Array.isArray(lore)) {
    candidates.push(lore.map(componentText).join('\n'))
  }

  if (item.components) {
    candidates.push(componentText(item.components))
    candidates.push(JSON.stringify(item.components))
  }
  if (item.nbt) {
    candidates.push(JSON.stringify(item.nbt))
  }
  return candidates.filter(Boolean).join('\n')
}

function numericCustomModelDataValue(value) {
  if (!value) return null
  if (typeof value === 'number') return value
  if (Array.isArray(value)) {
    for (const entry of value) {
      const found = numericCustomModelDataValue(entry)
      if (found !== null) return found
    }
    return null
  }
  if (typeof value !== 'object') return null

  for (const entry of Object.values(value)) {
    const found = numericCustomModelDataValue(entry)
    if (found !== null) return found
  }
  return null
}

function findCustomModelDataValue(value) {
  if (!value || typeof value !== 'object') return null

  for (const [key, entry] of Object.entries(value)) {
    const normalizedKey = key.toLowerCase().replaceAll('-', '_')
    if (normalizedKey.includes('custom_model_data') || normalizedKey.includes('custommodeldata')) {
      const found = numericCustomModelDataValue(entry)
      if (found !== null) return found
    }
  }

  for (const entry of Object.values(value)) {
    const found = findCustomModelDataValue(entry)
    if (found !== null) return found
  }
  return null
}

function getWindowItemCustomModelData(item) {
  if (!item) return null
  const component = item.components?.find?.((candidate) => {
    const type = String(candidate.type || '').toLowerCase()
    return type === 'minecraft:custom_model_data' || type === 'custom_model_data'
  })
  if (component) {
    const found = numericCustomModelDataValue(component.data)
    if (found !== null) return found
  }
  return findCustomModelDataValue(item.nbt)
}

function summarizeWindowSlots(window) {
  if (!window) return []
  return window.slots
    .map((item, slot) => {
      if (!item) return null
      return {
        slot,
        name: item.name,
        displayName: getWindowItemDisplayName(item),
        lore: getWindowItemLoreText(item),
        customModelData: getWindowItemCustomModelData(item),
        customName: item.customName,
        components: item.components
      }
    })
    .filter(Boolean)
}

function findMailboxItemSlot(window, senderName) {
  for (let slot = 0; slot < window.slots.length; slot += 1) {
    const item = window.slots[slot]
    const displayName = getWindowItemDisplayName(item)
    if (displayName.includes(senderName) && MAILBOX_PACKAGE_SLOTS.includes(slot)) {
      return slot
    }
  }
  return -1
}

function findMailboxItemSlots(window, senderName) {
  const slots = []
  if (!window) return slots
  for (let slot = 0; slot < window.slots.length; slot += 1) {
    const item = window.slots[slot]
    const displayName = getWindowItemDisplayName(item)
    if (displayName.includes(senderName) && MAILBOX_PACKAGE_SLOTS.includes(slot)) {
      slots.push(slot)
    }
  }
  return slots
}

function assertMailboxLayout(window, expectedPackageSlots, options = {}) {
  const {
    expectPrevious = false,
    expectNext = false,
    senderName = BOT_USERNAME
  } = options

  const actualPackageSlots = findMailboxItemSlots(window, senderName)
  assert.deepEqual(actualPackageSlots, expectedPackageSlots)

  for (const slot of expectedPackageSlots) {
    assert.ok(MAILBOX_PACKAGE_SLOTS.includes(slot), `Package item rendered outside mailbox grid at slot ${slot}`)
    assert.equal(window.slots[slot]?.name, 'bundle', `Package item at slot ${slot} should use bundle material`)
    assert.equal(
      getWindowItemCustomModelData(window.slots[slot]),
      CUSTOM_MODEL_DATA.mailItem,
      `Package item at slot ${slot} should use CustomModelData ${CUSTOM_MODEL_DATA.mailItem}`
    )
  }
  for (const slot of MAILBOX_RESERVED_EMPTY_SLOTS) {
    assert.equal(window.slots[slot], null, `Reserved separator slot ${slot} should stay empty`)
  }

  const previousName = getWindowItemDisplayName(window.slots[27])
  const nextName = getWindowItemDisplayName(window.slots[35])
  assert.equal(previousName.includes('上一页'), expectPrevious, `Previous control mismatch at slot 27: ${previousName}`)
  assert.equal(nextName.includes('下一页'), expectNext, `Next control mismatch at slot 35: ${nextName}`)

  if (expectPrevious) assertNavigationLore(window.slots[27], '上一页')
  if (expectNext) assertNavigationLore(window.slots[35], '下一页')
  if (expectPrevious) assert.equal(getWindowItemCustomModelData(window.slots[27]), CUSTOM_MODEL_DATA.previous)
  if (expectNext) assert.equal(getWindowItemCustomModelData(window.slots[35]), CUSTOM_MODEL_DATA.next)
}

function assertNavigationLore(item, displayName) {
  const name = getWindowItemDisplayName(item)
  const lore = getWindowItemLoreText(item)
  assert.ok(name.includes(displayName), `Expected navigation display name to include ${displayName}; actual=${name}`)
  assert.ok(lore.includes('邮箱导航'), `Expected navigation lore to include 邮箱导航; lore=${lore}`)
  assert.ok(lore.includes('当前页'), `Expected navigation lore to include 当前页; lore=${lore}`)
  assert.ok(lore.includes('点击切换'), `Expected navigation lore to include 点击切换; lore=${lore}`)
}

function assertMailboxItemLore(window, slot, senderName, messageTextFragment) {
  const item = window.slots[slot]
  const displayName = getWindowItemDisplayName(item)
  const lore = getWindowItemLoreText(item)
  assert.ok(displayName.includes(senderName), `Expected mail display name to include sender ${senderName}; actual=${displayName}`)
  assert.ok(displayName.includes('邮件'), `Expected localized mail display name; actual=${displayName}`)
  for (const expected of ['邮件编号', '寄件人', '状态', '发送时间', '过期时间', '邮件内容', '左键点击领取附件']) {
    assert.ok(lore.includes(expected), `Expected mail lore to include ${expected}; lore=${lore}`)
  }
  assert.ok(lore.includes(senderName), `Expected mail lore to include sender ${senderName}; lore=${lore}`)
  assert.ok(lore.includes(messageTextFragment), `Expected mail lore to include message fragment ${messageTextFragment}; lore=${lore}`)
}

async function waitForWindowWithMailboxItem(bot, senderName, maxTicks = 100) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    const window = bot.currentWindow
    if (window && findMailboxItemSlot(window, senderName) >= 0) {
      return window
    }
    await waitForTicksOrThrow(bot, 1)
  }

  const title = bot.currentWindow ? JSON.stringify(bot.currentWindow.title) : '<none>'
  throw new Error(`Timed out waiting for mailbox GUI item from ${senderName}; currentWindow=${title}; slots=${JSON.stringify(summarizeWindowSlots(bot.currentWindow), null, 2)}`)
}

async function openMailboxWindow(bot) {
  const windowOpenPromise = once(bot, 'windowOpen')
  bot.chat('/rookiepostbox menu')
  await windowOpenPromise
  await waitForTicksOrThrow(bot, 10)
  return bot.currentWindow
}

async function waitForWindowTitle(bot, titleMatcher, maxTicks = 100) {
  for (let tick = 0; tick < maxTicks; tick += 1) {
    const title = bot.currentWindow ? JSON.stringify(bot.currentWindow.title) : ''
    if (titleMatcher.test(title)) return bot.currentWindow
    await waitForTicksOrThrow(bot, 1)
  }
  throw new Error(`Timed out waiting for window title ${titleMatcher}; currentWindow=${bot.currentWindow ? JSON.stringify(bot.currentWindow.title) : '<none>'}`)
}

function findCurrentWindowItemSlot(bot, itemName) {
  const window = bot.currentWindow
  if (!window) return -1
  return window.slots.findIndex((item) => item && item.name === itemName)
}

async function moveItemToAttachmentSlot(bot, itemName, attachmentSlot = 0) {
  for (let tick = 0; tick < 60; tick += 1) {
    const sourceSlot = findCurrentWindowItemSlot(bot, itemName)
    if (sourceSlot >= 0) {
      await bot.moveSlotItem(sourceSlot, attachmentSlot)
      await waitForTicksOrThrow(bot, 5)
      return
    }
    await waitForTicksOrThrow(bot, 1)
  }
  throw new Error(`Could not find ${itemName} in current window slots`)
}

async function openComposeAttachOneItemAndConfirm(bot, command, itemName, successMessageMatcher) {
  const windowOpenPromise = once(bot, 'windowOpen')
  bot.chat(command)
  await windowOpenPromise
  await waitForWindowTitle(bot, /Attach Mail Items/)
  await moveItemToAttachmentSlot(bot, itemName, 0)
  const successPromise = waitForMessage(bot, successMessageMatcher, 120)
  await bot.clickWindow(21, 0, 0)
  await successPromise
  await waitForTicksOrThrow(bot, 10)
}

async function grantOneItemMail(adminBot, recipientName, itemCommandName, inventoryItemName, message) {
  adminBot.chat(`/give ${BOT_USERNAME} ${itemCommandName} 1`)
  await waitForInventoryCountAtLeast(adminBot, inventoryItemName, 1)
  await openComposeAttachOneItemAndConfirm(
    adminBot,
    `/rookiepostbox admin grant ${recipientName} ${message}`,
    inventoryItemName,
    /Admin grant succeeded\. Package #\d+/
  )
}

function findFirstEmptyPlayerWindowSlot(bot) {
  const window = bot.currentWindow
  if (!window) return -1
  for (let slot = window.inventoryStart; slot < window.inventoryEnd; slot += 1) {
    if (!window.slots[slot]) return slot
  }
  return -1
}

async function openMailDetailWindow(bot, senderName) {
  const mailboxWindow = await openMailboxWindow(bot)
  const packageSlot = findMailboxItemSlot(mailboxWindow, senderName)
  assert.notEqual(packageSlot, -1, 'Expected detail test package to be present')

  const detailOpenPromise = once(bot, 'windowOpen')
  await bot.clickWindow(packageSlot, 0, 0)
  await detailOpenPromise
  await waitForTicksOrThrow(bot, 10)
  const detailWindow = await waitForWindowTitle(bot, /邮件消息|閭欢娑堟伅|\\u90ae\\u4ef6\\u6d88\\u606f/, 120)
  const firstAttachmentSlot = DETAIL_ATTACHMENT_SLOTS[0]
  assert.equal(detailWindow.slots[firstAttachmentSlot]?.name, 'amethyst_shard', `Expected attachment at detail slot ${firstAttachmentSlot}; slots=${JSON.stringify(summarizeWindowSlots(detailWindow), null, 2)}`)
  return detailWindow
}

async function runMailDetailGuiClaimTest() {
  const adminBot = createBot(BOT_USERNAME)
  const recipient = createBot(DETAIL_RECIPIENT_USERNAME)
  const bots = [adminBot, recipient]

  try {
    await Promise.all(bots.map((bot) => once(bot, 'spawn')))
    bots.forEach((bot) => assert.equal(bot.lastError, undefined))
    await Promise.all(bots.map((bot) => waitForTicksOrThrow(bot, 20)))

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${DETAIL_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'amethyst_shard', 0),
      waitForInventoryCount(recipient, 'amethyst_shard', 0)
    ])

    await grantOneItemMail(
      adminBot,
      DETAIL_RECIPIENT_USERNAME,
      DETAIL_TEST_ITEM,
      'amethyst_shard',
      `detail-close-${RUN_SUFFIX}`
    )

    await openMailDetailWindow(recipient, BOT_USERNAME)
    assert.equal(countInventoryItems(recipient, 'amethyst_shard'), 0)
    recipient.closeWindow(recipient.currentWindow)
    await waitForInventoryCount(recipient, 'amethyst_shard', 1, 120)

    await grantOneItemMail(
      adminBot,
      DETAIL_RECIPIENT_USERNAME,
      DETAIL_TEST_ITEM,
      'amethyst_shard',
      `detail-drag-${RUN_SUFFIX}`
    )

    await openMailDetailWindow(recipient, BOT_USERNAME)
    const destinationSlot = findFirstEmptyPlayerWindowSlot(recipient)
    assert.notEqual(destinationSlot, -1, 'Expected empty player inventory slot in detail GUI')
    await recipient.clickWindow(DETAIL_ATTACHMENT_SLOTS[0], 0, 0)
    await waitForWindowSlotEmpty(recipient, DETAIL_ATTACHMENT_SLOTS[0], 120)
    await recipient.clickWindow(destinationSlot, 0, 0)
    recipient.closeWindow(recipient.currentWindow)
    await waitForInventoryCount(recipient, 'amethyst_shard', 2, 120)

    const reopenedWindow = await openMailboxWindow(recipient)
    assert.equal(findMailboxItemSlot(reopenedWindow, BOT_USERNAME), -1, 'Detail-claimed packages should not remain claimable')

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'mail detail GUI claim and attachment movement',
      recipient: DETAIL_RECIPIENT_USERNAME,
      detailTitle: '邮件消息',
      attachmentSlots: DETAIL_ATTACHMENT_SLOTS,
      autoReturnedCount: 1,
      draggedDestinationSlot: destinationSlot,
      finalAmethystCount: countInventoryItems(recipient, 'amethyst_shard')
    }, null, 2))
  } finally {
    bots.forEach((bot) => bot.quit())
  }
}

async function claimLatestMailFrom(bot, senderName, expectedReceivedMessage, itemName, expectedMinimumCount) {
  const windowOpenPromise = once(bot, 'windowOpen')
  bot.chat('/rookiepostbox menu')
  await windowOpenPromise

  await waitForTicksOrThrow(bot, 10)
  const mailboxWindow = await waitForWindowWithMailboxItem(bot, senderName)
  const packageSlot = findMailboxItemSlot(mailboxWindow, senderName)
  assert.notEqual(packageSlot, -1, `Expected mailbox package item from ${senderName} to be present in GUI`)

  const claimStartPromise = waitForMessage(bot, /Mailbox >>> Claiming package \d+/, 100)
  const receivedItemPromise = waitForMessage(bot, expectedReceivedMessage, 100)
  await bot.clickWindow(packageSlot, 0, 1)
  await claimStartPromise
  await receivedItemPromise

  return waitForInventoryCountAtLeast(bot, itemName, expectedMinimumCount)
}

async function claimCurrentWindowSlot(bot, slot, expectedReceivedMessage, itemName, expectedMinimumCount) {
  const claimStartPromise = waitForMessage(bot, /Mailbox >>> Claiming package \d+/, 100)
  const receivedItemPromise = waitForMessage(bot, expectedReceivedMessage, 100)
  await bot.clickWindow(slot, 0, 1)
  await claimStartPromise
  await receivedItemPromise
  await waitForInventoryCountAtLeast(bot, itemName, expectedMinimumCount)
  await waitForTicksOrThrow(bot, 20)
  return bot.currentWindow
}

async function runMailboxPaginationLayoutTest() {
  const adminBot = createBot(BOT_USERNAME)
  const pageRecipient = createBot(PAGE_RECIPIENT_USERNAME)
  const bots = [adminBot, pageRecipient]

  try {
    await Promise.all(bots.map((bot) => once(bot, 'spawn')))
    bots.forEach((bot) => assert.equal(bot.lastError, undefined))
    await Promise.all(bots.map((bot) => waitForTicksOrThrow(bot, 20)))

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${PAGE_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'copper_ingot', 0),
      waitForInventoryCount(pageRecipient, 'copper_ingot', 0)
    ])

    for (let index = 0; index < MAILBOX_PACKAGE_SLOTS.length + 2; index += 1) {
      await grantOneItemMail(
        adminBot,
        PAGE_RECIPIENT_USERNAME,
        PAGE_TEST_ITEM,
        'copper_ingot',
        `page-${RUN_SUFFIX}-${index}`
      )
    }

    let mailboxWindow = await openMailboxWindow(pageRecipient)
    assertMailboxLayout(mailboxWindow, MAILBOX_PACKAGE_SLOTS, {
      expectPrevious: false,
      expectNext: true,
      senderName: BOT_USERNAME
    })
    assertMailboxItemLore(mailboxWindow, 19, BOT_USERNAME, `page-${RUN_SUFFIX}`)

    await pageRecipient.clickWindow(35, 0, 0)
    await waitForTicksOrThrow(pageRecipient, 12)
    mailboxWindow = pageRecipient.currentWindow
    assertMailboxLayout(mailboxWindow, [19, 20], {
      expectPrevious: true,
      expectNext: false,
      senderName: BOT_USERNAME
    })

    await pageRecipient.clickWindow(27, 0, 0)
    await waitForTicksOrThrow(pageRecipient, 12)
    assertMailboxLayout(pageRecipient.currentWindow, MAILBOX_PACKAGE_SLOTS, {
      expectPrevious: false,
      expectNext: true,
      senderName: BOT_USERNAME
    })

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'mailbox GUI layout and pagination',
      recipient: PAGE_RECIPIENT_USERNAME,
      expectedFirstPagePackageSlots: MAILBOX_PACKAGE_SLOTS,
      expectedSecondPagePackageSlots: [19, 20],
      previousControlSlot: 27,
      nextControlSlot: 35,
      generatedMailCount: MAILBOX_PACKAGE_SLOTS.length + 2
    }, null, 2))
  } finally {
    bots.forEach((bot) => bot.quit())
  }
}

async function runRapidClickClaimExploitTest() {
  const adminBot = createBot(BOT_USERNAME)
  const recipient = createBot(RAPID_RECIPIENT_USERNAME)
  const bots = [adminBot, recipient]

  try {
    await Promise.all(bots.map((bot) => once(bot, 'spawn')))
    bots.forEach((bot) => assert.equal(bot.lastError, undefined))
    await Promise.all(bots.map((bot) => waitForTicksOrThrow(bot, 20)))

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${RAPID_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'gold_ingot', 0),
      waitForInventoryCount(recipient, 'gold_ingot', 0)
    ])

    await grantOneItemMail(
      adminBot,
      RAPID_RECIPIENT_USERNAME,
      RAPID_TEST_ITEM,
      'gold_ingot',
      `rapid-${RUN_SUFFIX}`
    )

    const mailboxWindow = await openMailboxWindow(recipient)
    const packageSlot = findMailboxItemSlot(mailboxWindow, BOT_USERNAME)
    assert.notEqual(packageSlot, -1, 'Expected rapid-click test package to be present')

    const receivedItemPromise = waitForMessage(recipient, /Mailbox >>> Received GOLD[ _]INGOT x 1/, 120)
    const clickResults = await Promise.allSettled(
      Array.from({ length: 8 }, () => recipient.clickWindow(packageSlot, 0, 1))
    )
    await receivedItemPromise
    await waitForTicksOrThrow(recipient, 20)
    const finalGoldCount = countInventoryItems(recipient, 'gold_ingot')
    assert.equal(finalGoldCount, 1)

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'rapid click claim duplicate prevention',
      recipient: RAPID_RECIPIENT_USERNAME,
      packageSlot,
      attemptedClicks: clickResults.length,
      rejectedClientClicks: clickResults.filter((result) => result.status === 'rejected').length,
      finalGoldCount
    }, null, 2))
  } finally {
    bots.forEach((bot) => bot.quit())
  }
}

async function runMailboxClaimPageBoundaryTest() {
  const adminBot = createBot(BOT_USERNAME)
  const boundaryRecipient = createBot(BOUNDARY_RECIPIENT_USERNAME)
  const emptyRecipient = createBot(EMPTY_RECIPIENT_USERNAME)
  const bots = [adminBot, boundaryRecipient, emptyRecipient]

  try {
    await Promise.all(bots.map((bot) => once(bot, 'spawn')))
    bots.forEach((bot) => assert.equal(bot.lastError, undefined))
    await Promise.all(bots.map((bot) => waitForTicksOrThrow(bot, 20)))

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${BOUNDARY_RECIPIENT_USERNAME}`)
    adminBot.chat(`/clear ${EMPTY_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'lapis_lazuli', 0),
      waitForInventoryCount(adminBot, 'redstone', 0),
      waitForInventoryCount(boundaryRecipient, 'lapis_lazuli', 0),
      waitForInventoryCount(emptyRecipient, 'redstone', 0)
    ])

    for (let index = 0; index < MAILBOX_PACKAGE_SLOTS.length + 1; index += 1) {
      await grantOneItemMail(
        adminBot,
        BOUNDARY_RECIPIENT_USERNAME,
        BOUNDARY_TEST_ITEM,
        'lapis_lazuli',
        `boundary-last-page-${RUN_SUFFIX}-${index}`
      )
    }
    await grantOneItemMail(
      adminBot,
      EMPTY_RECIPIENT_USERNAME,
      EMPTY_TEST_ITEM,
      'redstone',
      `boundary-empty-${RUN_SUFFIX}`
    )

    let mailboxWindow = await openMailboxWindow(boundaryRecipient)
    assertMailboxLayout(mailboxWindow, MAILBOX_PACKAGE_SLOTS, {
      expectPrevious: false,
      expectNext: true,
      senderName: BOT_USERNAME
    })

    await boundaryRecipient.clickWindow(35, 0, 0)
    await waitForTicksOrThrow(boundaryRecipient, 12)
    mailboxWindow = boundaryRecipient.currentWindow
    assertMailboxLayout(mailboxWindow, [19], {
      expectPrevious: true,
      expectNext: false,
      senderName: BOT_USERNAME
    })

    await claimCurrentWindowSlot(
      boundaryRecipient,
      19,
      /Mailbox >>> Received LAPIS[ _]LAZULI x 1/,
      'lapis_lazuli',
      1
    )
    mailboxWindow = await waitForWindowWithMailboxItem(boundaryRecipient, BOT_USERNAME, 160)
    assertMailboxLayout(mailboxWindow, MAILBOX_PACKAGE_SLOTS, {
      expectPrevious: false,
      expectNext: false,
      senderName: BOT_USERNAME
    })

    const emptyMailboxWindow = await openMailboxWindow(emptyRecipient)
    assertMailboxLayout(emptyMailboxWindow, [19], {
      expectPrevious: false,
      expectNext: false,
      senderName: BOT_USERNAME
    })
    const afterEmptyClaimWindow = await claimCurrentWindowSlot(
      emptyRecipient,
      19,
      /Mailbox >>> Received REDSTONE x 1/,
      'redstone',
      1
    )
    assertMailboxLayout(afterEmptyClaimWindow, [], {
      expectPrevious: false,
      expectNext: false,
      senderName: BOT_USERNAME
    })

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'mailbox claim page boundaries',
      boundaryRecipient: BOUNDARY_RECIPIENT_USERNAME,
      lastPageBeforeClaimSlots: [19],
      afterLastPageOnlyClaimSlots: MAILBOX_PACKAGE_SLOTS,
      afterLastPageOnlyClaimHasPrevious: false,
      afterLastPageOnlyClaimHasNext: false,
      emptyRecipient: EMPTY_RECIPIENT_USERNAME,
      afterSingleMailClaimSlots: [],
      afterSingleMailClaimHasPrevious: false,
      afterSingleMailClaimHasNext: false
    }, null, 2))
  } finally {
    bots.forEach((bot) => bot.quit())
  }
}

async function runJoinUnreadNotificationTest() {
  const adminBot = createBot(BOT_USERNAME)
  let recipient = createBot(NOTIFY_RECIPIENT_USERNAME)

  try {
    await Promise.all([once(adminBot, 'spawn'), once(recipient, 'spawn')])
    assert.equal(adminBot.lastError, undefined)
    assert.equal(recipient.lastError, undefined)
    await Promise.all([waitForTicksOrThrow(adminBot, 20), waitForTicksOrThrow(recipient, 20)])

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${NOTIFY_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'quartz', 0),
      waitForInventoryCount(recipient, 'quartz', 0)
    ])

    recipient.quit()
    await once(recipient, 'end')

    await grantOneItemMail(
      adminBot,
      NOTIFY_RECIPIENT_USERNAME,
      NOTIFY_TEST_ITEM,
      'quartz',
      `notify-${RUN_SUFFIX}`
    )

    recipient = createBot(NOTIFY_RECIPIENT_USERNAME)
    await once(recipient, 'spawn')
    const notification = await waitForMessage(recipient, /邮箱提醒.*1.*未读邮件/, 160)

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'join unread mail notification',
      recipient: NOTIFY_RECIPIENT_USERNAME,
      notification
    }, null, 2))
  } finally {
    adminBot.quit()
    recipient.quit()
  }
}

async function runDisconnectClaimExploitTest() {
  const adminBot = createBot(BOT_USERNAME)
  let recipient = createBot(DISCONNECT_RECIPIENT_USERNAME)

  try {
    await Promise.all([once(adminBot, 'spawn'), once(recipient, 'spawn')])
    assert.equal(adminBot.lastError, undefined)
    assert.equal(recipient.lastError, undefined)
    await Promise.all([waitForTicksOrThrow(adminBot, 20), waitForTicksOrThrow(recipient, 20)])

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${DISCONNECT_RECIPIENT_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'iron_ingot', 0),
      waitForInventoryCount(recipient, 'iron_ingot', 0)
    ])

    await grantOneItemMail(
      adminBot,
      DISCONNECT_RECIPIENT_USERNAME,
      DISCONNECT_TEST_ITEM,
      'iron_ingot',
      `disconnect-${RUN_SUFFIX}`
    )

    const mailboxWindow = await openMailboxWindow(recipient)
    const packageSlot = findMailboxItemSlot(mailboxWindow, BOT_USERNAME)
    assert.notEqual(packageSlot, -1, 'Expected disconnect test package to be present')

    const receivedItemPromise = waitForMessage(recipient, /Mailbox >>> Received IRON[ _]INGOT x 1/, 120)
    await recipient.clickWindow(packageSlot, 0, 1)
    await receivedItemPromise
    recipient.quit()
    await once(recipient, 'end')

    recipient = createBot(DISCONNECT_RECIPIENT_USERNAME)
    await once(recipient, 'spawn')
    await waitForTicksOrThrow(recipient, 30)
    const finalIronCount = await waitForInventoryCount(recipient, 'iron_ingot', 1, 120)

    const reopenedWindow = await openMailboxWindow(recipient)
    const remainingPackageSlot = findMailboxItemSlot(reopenedWindow, BOT_USERNAME)
    assert.equal(remainingPackageSlot, -1, 'Claimed package should not remain claimable after reconnect')

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'disconnect after claim duplicate prevention',
      recipient: DISCONNECT_RECIPIENT_USERNAME,
      packageSlot,
      finalIronCount,
      remainingPackageSlot
    }, null, 2))
  } finally {
    adminBot.quit()
    recipient.quit()
  }
}

async function runRookiePostBoxIntegrationTest() {
  const bot = createBot()

  try {
    await once(bot, 'spawn')
    assert.equal(bot.lastError, undefined)

    await waitForTicksOrThrow(bot, 20)

    bot.chat('/clear @s')
    await waitForInventoryCount(bot, 'diamond', 0)
    await waitForTicksOrThrow(bot, 5)

    await sendCommandAndWaitForMessage(bot, '/rookiepostbox unknown', /Unknown subcommand\./)
    await sendCommandAndWaitForMessage(bot, '/rookiepostbox reload', /RookiePostBox config reloaded\./)
    const emptyComposeOpenPromise = once(bot, 'windowOpen')
    bot.chat(`/rookiepostbox save ${TEST_MESSAGE}-empty`)
    await emptyComposeOpenPromise
    await waitForWindowTitle(bot, /Attach Mail Items/)
    const emptyAttachmentPromise = waitForMessage(bot, /Mailbox >>> Add at least one attachment\./)
    await bot.clickWindow(21, 0, 0)
    await emptyAttachmentPromise
    bot.closeWindow(bot.currentWindow)
    await waitForTicksOrThrow(bot, 10)

    bot.chat(`/give ${BOT_USERNAME} ${TEST_ITEM} 1`)
    await waitForInventoryCountAtLeast(bot, 'diamond', 1)

    await openComposeAttachOneItemAndConfirm(
      bot,
      `/rookiepostbox save ${TEST_MESSAGE}`,
      'diamond',
      /Mailbox >>> Mail sent\. recipients=1, failed=0/
    )
    await waitForInventoryCount(bot, 'diamond', 0)

    const finalDiamondCount = await claimLatestMailFrom(
      bot,
      BOT_USERNAME,
      /Mailbox >>> Received DIAMOND x 1/,
      'diamond',
      1
    )
    assert.equal(finalDiamondCount, 1)

    console.log(JSON.stringify({
      status: 'passed',
      server: `${SERVER_HOST}:${SERVER_PORT}`,
      minecraftVersion: MINECRAFT_VERSION,
      botUsername: BOT_USERNAME,
      covered: [
        'unknown subcommand feedback',
        'admin config reload command',
        'compose GUI empty-attachment feedback',
        'give test fixture item',
        'attach item through compose GUI',
        'confirm compose GUI send',
        'open mailbox GUI',
        'click mailbox package item',
        'claim feedback',
        'inventory item count assertion'
      ],
      finalDiamondCount
    }, null, 2))
  } finally {
    bot.quit()
  }
}

async function runAdminGrantAllIntegrationTest() {
  const adminBot = createBot(BOT_USERNAME)
  const recipientA = createBot(RECIPIENT_A_USERNAME)
  const recipientB = createBot(RECIPIENT_B_USERNAME)
  const bots = [adminBot, recipientA, recipientB]

  try {
    await Promise.all(bots.map((bot) => once(bot, 'spawn')))
    bots.forEach((bot) => assert.equal(bot.lastError, undefined))

    await Promise.all(bots.map((bot) => waitForTicksOrThrow(bot, 20)))

    adminBot.chat(`/clear ${BOT_USERNAME}`)
    adminBot.chat(`/clear ${RECIPIENT_A_USERNAME}`)
    adminBot.chat(`/clear ${RECIPIENT_B_USERNAME}`)
    await Promise.all([
      waitForInventoryCount(adminBot, 'emerald', 0),
      waitForInventoryCount(recipientA, 'emerald', 0),
      waitForInventoryCount(recipientB, 'emerald', 0)
    ])

    adminBot.chat(`/give ${BOT_USERNAME} ${BULK_TEST_ITEM} 1`)
    await waitForInventoryCountAtLeast(adminBot, 'emerald', 1)

    await openComposeAttachOneItemAndConfirm(
      adminBot,
      `/rookiepostbox admin grantall ${BULK_TEST_MESSAGE}`,
      'emerald',
      /Admin grantall completed\. recipients=2, failed=0/,
    )
    await waitForInventoryCount(adminBot, 'emerald', 0)

    const recipientAEmeraldCount = await claimLatestMailFrom(
      recipientA,
      BOT_USERNAME,
      /Mailbox >>> Received EMERALD x 1/,
      'emerald',
      1
    )
    const recipientBEmeraldCount = await claimLatestMailFrom(
      recipientB,
      BOT_USERNAME,
      /Mailbox >>> Received EMERALD x 1/,
      'emerald',
      1
    )

    assert.equal(recipientAEmeraldCount, 1)
    assert.equal(recipientBEmeraldCount, 1)

    console.log(JSON.stringify({
      status: 'passed',
      scenario: 'admin grantall online recipients',
      server: `${SERVER_HOST}:${SERVER_PORT}`,
      minecraftVersion: MINECRAFT_VERSION,
      adminUsername: BOT_USERNAME,
      recipients: [RECIPIENT_A_USERNAME, RECIPIENT_B_USERNAME],
      covered: [
        'admin grantall command',
        'online recipient fan-out',
        'admin attachment consumed after grantall confirm',
        'recipient A mailbox GUI claim',
        'recipient B mailbox GUI claim',
        'recipient inventory item count assertions'
      ],
      recipientAEmeraldCount,
      recipientBEmeraldCount
    }, null, 2))
  } finally {
    bots.forEach((bot) => bot.quit())
  }
}

async function main() {
  await runRookiePostBoxIntegrationTest()
  await runAdminGrantAllIntegrationTest()
  await runMailDetailGuiClaimTest()
  await runMailboxPaginationLayoutTest()
  await runMailboxClaimPageBoundaryTest()
  await runJoinUnreadNotificationTest()
  await runRapidClickClaimExploitTest()
  await runDisconnectClaimExploitTest()
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
