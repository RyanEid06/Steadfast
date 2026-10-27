import bibleBooksJson from './bible_books.json'
import milestonesJson from './milestones.json'
import recoveryFocusesJson from './recovery_focuses.json'
import verseCatalogJson from './verse_reference_catalog_starter.json'
import {
  BibleBook,
  MilestoneDefinition,
  RecoveryFocus,
  ScaleComparison,
  CuratedVerse,
} from '../types'

export const BIBLE_BOOKS: BibleBook[] = bibleBooksJson.books as BibleBook[]
export const MILESTONES: MilestoneDefinition[] = milestonesJson.milestones as MilestoneDefinition[]
export const RECOVERY_FOCUSES: RecoveryFocus[] = recoveryFocusesJson.items as RecoveryFocus[]

export const SCALE_COMPARISONS: ScaleComparison[] = [
  {
    id: 'sc_1',
    thresholdDays: 1,
    title: 'The First Step of Faith',
    description:
      'You have completed 24 hours of intentional surrender. The journey of a thousand leagues begins beneath your feet.',
    scriptureReference:
      "Zechariah 4:10 — 'Do not despise these small beginnings, for the Lord rejoices to see the work begin.'",
  },
  {
    id: 'sc_3',
    thresholdDays: 3,
    title: 'The Resurrected Morning',
    description:
      'Three days: the span Christ rested in the tomb before rising in victory. You are stepping into new life.',
    scriptureReference:
      "Hosea 6:2 — 'After two days he will revive us; on the third day he will restore us, that we may live in his presence.'",
  },
  {
    id: 'sc_7',
    thresholdDays: 7,
    title: 'A Sabbath of Freedom',
    description:
      '168 unbroken hours. God created the heavens and the earth and rested on the seventh day. A full week consecrated to freedom.',
    scriptureReference:
      "Genesis 2:3 — 'And God blessed the seventh day and declared it holy, because on it he rested from all his work.'",
  },
  {
    id: 'sc_14',
    thresholdDays: 14,
    title: 'Fortified Resolve',
    description:
      'Two complete cycles of weeks. Your mind and neural pathways are clearing as grace replaces compulsive impulses.',
    scriptureReference:
      "Psalm 31:24 — 'Be strong, and let your heart take courage, all you who wait for the Lord!'",
  },
  {
    id: 'sc_21',
    thresholdDays: 21,
    title: 'The Daniel Fast',
    description:
      "Three weeks of steady spiritual devotion, mirroring Daniel's faithful fasting before the angel brought God's message.",
    scriptureReference:
      "Daniel 10:2-3 — 'At that time I, Daniel, mourned for three weeks... until the three full weeks were over.'",
  },
  {
    id: 'sc_30',
    thresholdDays: 30,
    title: 'A Full Moon Cycle',
    description:
      'One complete month. Your timer now elevates into Months, Days, Hours, and Seconds.',
    scriptureReference:
      "Lamentations 3:22-23 — 'The faithful love of the Lord never ends! His mercies never cease. Great is his faithfulness; his mercies begin afresh each morning.'",
  },
  {
    id: 'sc_60',
    thresholdDays: 60,
    title: 'Deep Rooted Strength',
    description:
      'Two months of perseverance. Your roots are sinking deep beside streams of living water.',
    scriptureReference:
      "Jeremiah 17:8 — 'They are like trees planted along a riverbank, with roots that reach deep into the water.'",
  },
  {
    id: 'sc_90',
    thresholdDays: 90,
    title: 'A Season Transformed',
    description:
      'A quarter of a calendar year. Old seasons pass away; new clarity and spiritual discipline take firm hold.',
    scriptureReference:
      "2 Corinthians 5:17 — 'This means that anyone who belongs to Christ has become a new person. The old life is gone; a new life has begun!'",
  },
  {
    id: 'sc_180',
    thresholdDays: 180,
    title: 'Half a Solar Year',
    description:
      'Half a year walking in steadfast grace. You have walked through changing weather and remained unmovable.',
    scriptureReference:
      "1 Corinthians 15:58 — 'Therefore, my beloved brethren, be steadfast, immovable, always abounding in the work of the Lord.'",
  },
  {
    id: 'sc_365',
    thresholdDays: 365,
    title: 'A Jubilee Year',
    description:
      'A full calendar year in the light. 365 days of answered prayers, overcome urges, and deepened devotion.',
    scriptureReference:
      "Isaiah 40:31 — 'Those who trust in the Lord will find new strength. They will soar high on wings like eagles; they will run and not grow weary; they will walk and not faint.'",
  },
]

export const SCRIPTURE_TEXTS: Record<string, string> = {
  '1 Corinthians 10:13':
    '“The temptations in your life are no different from what others experience. And God is faithful. He will not allow the temptation to be more than you can stand. When you are tempted, he will show you a way out so that you can endure.”',
  'James 1:12':
    '“God blesses those who patiently endure testing and temptation. Afterward they will receive the crown of life that God has promised to those who love him.”',
  'James 4:7':
    '“So humble yourselves before God. Resist the devil, and he will flee from you.”',
  'Matthew 26:41':
    '“Keep watch and pray, so that you will not give in to temptation. For the spirit is willing, but the body is weak.”',
  'Hebrews 2:18':
    '“Since he himself has gone through suffering and testing, he is able to help us when we are being tested.”',
  'Philippians 4:13':
    '“For I can do everything through Christ, who gives me strength.”',
  'Isaiah 40:31':
    '“But those who trust in the Lord will find new strength. They will soar high on wings like eagles; they will run and not grow weary; they will walk and not faint.”',
  'Psalm 28:7':
    '“The Lord is my strength and shield. I trust him with all my heart. He helps me, and my heart leaps for joy. I praise him with my song.”',
  '2 Corinthians 12:9':
    '“Each time he said, ‘My grace is all you need. My power works best in weakness.’ So now I am glad to boast about my weaknesses, so that the power of Christ can work through me.”',
  'Ephesians 6:10':
    '“A final word: Be strong in the Lord and in his mighty power.”',
  'John 3:16':
    '“For this is how God loved the world: He gave his one and only Son, so that everyone who believes in him will not perish but have eternal life.”',
  'Romans 8:38-39':
    '“And I am convinced that nothing can ever separate us from God’s love. Neither death nor life, neither angels nor demons, neither our fears for today nor our worries about tomorrow—not even the powers of hell can separate us from God’s love.”',
  '1 John 4:9-10':
    '“God showed how much he loved us by sending his one and only Son into the world so that we might have eternal life through him.”',
  'Psalm 136:1':
    '“Give thanks to the Lord, for he is good! His faithful love endures forever.”',
  'Zephaniah 3:17':
    '“For the Lord your God is living among you. He is a mighty savior. He will take delight in you with gladness. With his love, he will calm all your fears. He will rejoice over you with joyful songs.”',
  '1 John 1:9':
    '“But if we confess our sins to him, he is faithful and just to forgive us our sins and to cleanse us from all wickedness.”',
  'Psalm 103:12':
    '“He has removed our sins as far from us as the east is from the west.”',
  'Ephesians 1:7':
    '“He is so rich in kindness and grace that he purchased our freedom with the blood of his Son and forgave our sins.”',
  'Micah 7:18-19':
    '“Where is another God like you, who pardons the guilt of the remnant, overlooking the sins of his special people? You will trample our sins under your feet and throw them into the depths of the ocean!”',
  'Colossians 3:13':
    '“Make allowance for each other’s faults, and forgive anyone who offends you. Remember, the Lord forgave you, so you must forgive others.”',
  'Galatians 5:22-23':
    '“But the Holy Spirit produces this kind of fruit in our lives: love, joy, peace, patience, kindness, goodness, faithfulness, gentleness, and self-control. There is no law against these things!”',
  '2 Timothy 1:7':
    '“For God has not given us a spirit of fear and timidity, but of power, love, and self-discipline.”',
  'Proverbs 25:28':
    '“A person without self-control is like a city with broken-down walls.”',
  '1 Peter 5:8-9':
    '“Stay alert! Watch out for your great enemy, the devil. He prowls around like a roaring lion, looking for someone to devour. Stand firm against him, and be strong in your faith.”',
  'Titus 2:11-12':
    '“For the grace of God has been revealed, bringing salvation to all people. And we are instructed to turn from godless living and sinful pleasures, and to live in this evil world with wisdom, righteousness, and devotion to God.”',
  'John 14:27':
    '“I am leaving you with a gift—peace of mind and heart. And the peace I give is a gift the world cannot give. So don’t be troubled or afraid.”',
  'Philippians 4:6-7':
    '“Don’t worry about anything; instead, pray about everything. Tell God what you need, and thank him for all he has done. Then you will experience God’s peace, which exceeds anything we can understand.”',
  'Psalm 4:8':
    '“In peace I will lie down and sleep, for you alone, O Lord, will keep me safe.”',
  'Isaiah 26:3':
    '“You will keep in perfect peace all who trust in you, all whose thoughts are fixed on you!”',
  'Matthew 11:28-30':
    '“Come to me, all of you who are weary and carry heavy burdens, and I will give you rest. Take my yoke upon you. Let me teach you, because I am humble and gentle at heart, and you will find rest for your souls.”',
  'Romans 5:3-5':
    '“We can rejoice, too, when we run into problems and trials, for we know that they help us develop endurance. And endurance develops strength of character, and character strengthens our confident hope of salvation.”',
  'Hebrews 12:1-2':
    '“Let us strip off every weight that slows us down, especially the sin that so easily trips us up. And let us run with endurance the race God has set before us. We do this by keeping our eyes on Jesus.”',
  'Galatians 6:9':
    '“So let’s not get tired of doing what is good. At just the right time we will reap a harvest of blessing if we don’t give up.”',
  'James 1:2-4':
    '“Dear brothers and sisters, when troubles of any kind come your way, consider it an opportunity for great joy. For you know that when your faith is tested, your endurance has a chance to grow.”',
  'Psalm 37:23-24':
    '“The Lord directs the steps of the godly. He delights in every detail of their lives. Though they stumble, they will never fall, for the Lord holds them by the hand.”',
  'Romans 12:2':
    '“Don’t copy the behavior and customs of this world, but let God transform you into a new person by changing the way you think. Then you will learn to know God’s will for you, which is good and pleasing and perfect.”',
  '2 Corinthians 5:17':
    '“This means that anyone who belongs to Christ has become a new person. The old life is gone; a new life has begun!”',
  'Lamentations 3:22-23':
    '“The faithful love of the Lord never ends! His mercies never cease. Great is his faithfulness; his mercies begin afresh each morning.”',
  'Ezekiel 36:26':
    '“And I will give you a new heart, and I will put a new spirit in you. I will take out your stony, stubborn heart and give you a tender, responsive heart.”',
  'Colossians 3:9-10':
    '“Don’t lie to each other, for you have stripped off your old sinful nature and all its wicked deeds. Put on your new nature, and be renewed as you learn to know your Creator and become like him.”',
}

export function parseReference(reference: string, theme: string): CuratedVerse | null {
  const colonIdx = reference.lastIndexOf(':')
  if (colonIdx === -1) return null
  const versePart = reference.substring(colonIdx + 1).trim()
  const beforeColon = reference.substring(0, colonIdx).trim()
  const spaceIdx = beforeColon.lastIndexOf(' ')
  if (spaceIdx === -1) return null

  const bookPart = beforeColon.substring(0, spaceIdx).trim()
  const chapterPart = parseInt(beforeColon.substring(spaceIdx + 1).trim(), 10) || 1

  return {
    reference,
    bookName: bookPart,
    chapter: chapterPart,
    verseNumber: versePart,
    theme,
    text:
      SCRIPTURE_TEXTS[reference] ||
      '“Stand firm, and you will win life. Trust in the Lord with all your heart, and do not lean on your own understanding.”',
  }
}

export const ALL_CURATED_VERSES: CuratedVerse[] = []
Object.entries(
  verseCatalogJson.themes as Record<string, string[]>
).forEach(([theme, refs]) => {
  refs.forEach((ref) => {
    const parsed = parseReference(ref, theme)
    if (parsed) {
      ALL_CURATED_VERSES.push(parsed)
    }
  })
})
