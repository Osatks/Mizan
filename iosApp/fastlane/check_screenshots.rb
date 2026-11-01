# يتحقق قبل الرفع أن deliver (إصدار fastlane المثبّت في Gemfile.lock) يعرف نوع شاشة كل لقطة في fastlane/screenshots:
# لقطة واحدة بمقاس لا يعرفه تُلغي رفع اللقطات كلها. يطبع لكل لقطة نوعها كما سيرفعها deliver، ولا يقبل أكثر من 10 للنوع.
#
#   bundle exec ruby fastlane/check_screenshots.rb fastlane/screenshots [--require-ipad] [--move-unknown DIR]
#
#   --require-ipad      لكل لغة فيها لقطات: لقطات iPhone 6.9 (APP_IPHONE_67) و iPad 13 (APP_IPAD_PRO_3GEN_129) معاً،
#                       وهما ما يطلبه App Store لتطبيق عام
#   --move-unknown DIR  ما لا يعرفه deliver يُنقل إلى DIR/<اللغة> (ملفاً مرفقاً فقط) بدل أن يُفشل الفحص
require "fileutils"
require "fastlane"
require "deliver"

root = ARGV[0] or abort("usage: check_screenshots.rb <screenshots dir> [--require-ipad] [--move-unknown DIR]")
require_ipad = ARGV.include?("--require-ipad")
move_index = ARGV.index("--move-unknown")
move_dir = move_index ? ARGV[move_index + 1] : nil
types = Spaceship::ConnectAPI::AppScreenshotSet::DisplayType

errors = []
Dir.glob(File.join(root, "*")).select { |d| File.directory?(d) }.sort.each do |dir|
  locale = File.basename(dir)
  counts = Hash.new(0)
  Dir.glob(File.join(dir, "*.{png,jpg,jpeg}"), File::FNM_CASEFOLD).sort.each do |path|
    size = FastImage.size(path)&.join("x")
    type = Deliver::AppScreenshot.calculate_display_type(path)
    if type.nil?
      if move_dir
        dest = File.join(move_dir, locale)
        FileUtils.mkdir_p(dest)
        FileUtils.mv(path, dest)
        puts "#{locale}/#{File.basename(path)} #{size}: unknown to deliver #{Fastlane::VERSION}, kept as an artifact only (#{dest})"
      else
        errors << "#{locale}/#{File.basename(path)}: #{size} is not a screenshot size deliver #{Fastlane::VERSION} knows"
      end
      next
    end
    counts[type] += 1
    puts "#{locale}/#{File.basename(path)} #{size} -> #{type}"
  end
  counts.each { |t, n| errors << "#{locale}: #{n} screenshots for #{t} (App Store allows 10)" if n > 10 }
  next unless require_ipad && counts.any?

  errors << "#{locale}: no 6.9-inch iPhone screenshots (#{types::APP_IPHONE_67})" unless counts.key?(types::APP_IPHONE_67)
  errors << "#{locale}: no 13-inch iPad screenshots (#{types::APP_IPAD_PRO_3GEN_129}), required for an iPad app" unless counts.key?(types::APP_IPAD_PRO_3GEN_129)
end

abort("\n" + errors.join("\n")) unless errors.empty?
puts "OK (fastlane #{Fastlane::VERSION})"
