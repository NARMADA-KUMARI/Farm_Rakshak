-- Seed Indian Mandis (50 major agricultural market yards)
INSERT INTO mandis (name, state, district, latitude, longitude) VALUES
-- Maharashtra
('Nashik Mandi', 'Maharashtra', 'Nashik', 19.9975, 73.7898),
('Pune Market Yard', 'Maharashtra', 'Pune', 18.5204, 73.8567),
('Akola Mandi', 'Maharashtra', 'Akola', 20.7002, 77.0082),
('Nagpur Mandi', 'Maharashtra', 'Nagpur', 21.1458, 79.0882),
('Solapur Mandi', 'Maharashtra', 'Solapur', 17.6599, 75.9064),
('Kolhapur Market', 'Maharashtra', 'Kolhapur', 16.7050, 74.2433),
('Aurangabad Mandi', 'Maharashtra', 'Aurangabad', 19.8762, 75.3433),
('Ahmednagar Mandi', 'Maharashtra', 'Ahmednagar', 19.0948, 74.7480),
-- Telangana
('Hyderabad Mandi', 'Telangana', 'Hyderabad', 17.3850, 78.4867),
('Warangal Mandi', 'Telangana', 'Warangal', 17.9689, 79.5941),
('Karimnagar Mandi', 'Telangana', 'Karimnagar', 18.4386, 79.1288),
('Nizamabad Mandi', 'Telangana', 'Nizamabad', 18.6725, 78.0941),
-- Andhra Pradesh
('Vijayawada Mandi', 'Andhra Pradesh', 'Krishna', 16.5062, 80.6480),
('Guntur Mandi', 'Andhra Pradesh', 'Guntur', 16.3067, 80.4365),
('Kurnool Mandi', 'Andhra Pradesh', 'Kurnool', 15.8281, 78.0373),
-- Karnataka
('Hubli Mandi', 'Karnataka', 'Dharwad', 15.3647, 75.1240),
('Belgaum Mandi', 'Karnataka', 'Belgaum', 15.8497, 74.4977),
('Bangalore APMC', 'Karnataka', 'Bangalore', 12.9716, 77.5946),
('Raichur Mandi', 'Karnataka', 'Raichur', 16.2076, 77.3590),
-- Madhya Pradesh
('Indore Mandi', 'Madhya Pradesh', 'Indore', 22.7196, 75.8577),
('Bhopal Mandi', 'Madhya Pradesh', 'Bhopal', 23.2599, 77.4126),
('Jabalpur Mandi', 'Madhya Pradesh', 'Jabalpur', 23.1815, 79.9864),
('Ujjain Mandi', 'Madhya Pradesh', 'Ujjain', 23.1765, 75.7885),
-- Gujarat
('Ahmedabad APMC', 'Gujarat', 'Ahmedabad', 23.0225, 72.5714),
('Rajkot Mandi', 'Gujarat', 'Rajkot', 22.3039, 70.8022),
('Surat Mandi', 'Gujarat', 'Surat', 21.1702, 72.8311),
('Junagadh Mandi', 'Gujarat', 'Junagadh', 21.5222, 70.4579),
-- Rajasthan
('Jaipur Mandi', 'Rajasthan', 'Jaipur', 26.9124, 75.7873),
('Jodhpur Mandi', 'Rajasthan', 'Jodhpur', 26.2389, 73.0243),
('Kota Mandi', 'Rajasthan', 'Kota', 25.2138, 75.8648),
-- Uttar Pradesh
('Lucknow Mandi', 'Uttar Pradesh', 'Lucknow', 26.8467, 80.9462),
('Agra Mandi', 'Uttar Pradesh', 'Agra', 27.1767, 78.0081),
('Kanpur Mandi', 'Uttar Pradesh', 'Kanpur', 26.4499, 80.3319),
('Varanasi Mandi', 'Uttar Pradesh', 'Varanasi', 25.3176, 82.9739),
-- Punjab
('Amritsar Mandi', 'Punjab', 'Amritsar', 31.6340, 74.8723),
('Ludhiana Grain Market', 'Punjab', 'Ludhiana', 30.9010, 75.8573),
('Jalandhar Mandi', 'Punjab', 'Jalandhar', 31.3260, 75.5762),
-- Haryana
('Karnal Mandi', 'Haryana', 'Karnal', 29.6857, 76.9905),
('Hisar Mandi', 'Haryana', 'Hisar', 29.1492, 75.7217),
-- Tamil Nadu
('Chennai Koyambedu', 'Tamil Nadu', 'Chennai', 13.0827, 80.2707),
('Coimbatore APMC', 'Tamil Nadu', 'Coimbatore', 11.0168, 76.9558),
('Madurai Mandi', 'Tamil Nadu', 'Madurai', 9.9252, 78.1198),
-- West Bengal
('Kolkata Mandi', 'West Bengal', 'Kolkata', 22.5726, 88.3639),
('Siliguri Mandi', 'West Bengal', 'Darjeeling', 26.7271, 88.3953),
-- Bihar
('Patna Mandi', 'Bihar', 'Patna', 25.6093, 85.1376),
-- Odisha
('Bhubaneswar Mandi', 'Odisha', 'Khurda', 20.2961, 85.8245),
-- Chhattisgarh
('Raipur Mandi', 'Chhattisgarh', 'Raipur', 21.2514, 81.6296),
-- Kerala
('Kochi APMC', 'Kerala', 'Ernakulam', 9.9312, 76.2673),
-- Assam
('Guwahati Mandi', 'Assam', 'Kamrup', 26.1445, 91.7362);

-- Seed Crop Master Database (30 major Indian crops)
INSERT INTO crops_master (crop_name, category, scientific_name, unit, local_names, synonyms) VALUES
('Tomato', 'Vegetable', 'Solanum lycopersicum', 'kg', ARRAY['Tamatar', 'Tamata', 'Thakkali', 'Tomato'], ARRAY['Tamatar', 'Tamata', 'Thakkali']),
('Onion', 'Vegetable', 'Allium cepa', 'kg', ARRAY['Pyaz', 'Kanda', 'Vengayam', 'Eerulli'], ARRAY['Pyaz', 'Kanda', 'Vengayam']),
('Potato', 'Vegetable', 'Solanum tuberosum', 'kg', ARRAY['Aloo', 'Batata', 'Urulaikizhangu'], ARRAY['Aloo', 'Batata']),
('Cotton', 'Cash Crop', 'Gossypium', 'quintal', ARRAY['Kapas', 'Rui', 'Paruthi'], ARRAY['Kapas', 'Rui', 'Paruthi']),
('Wheat', 'Cereal', 'Triticum aestivum', 'quintal', ARRAY['Gehu', 'Godhuma', 'Godhi'], ARRAY['Gehu', 'Godhuma']),
('Rice', 'Cereal', 'Oryza sativa', 'quintal', ARRAY['Chawal', 'Dhan', 'Arisi', 'Bhat'], ARRAY['Chawal', 'Dhan', 'Paddy']),
('Soybean', 'Oilseed', 'Glycine max', 'quintal', ARRAY['Soyabean', 'Bhat', 'Soya'], ARRAY['Soyabean', 'Soya']),
('Chili', 'Spice', 'Capsicum annuum', 'kg', ARRAY['Mirchi', 'Mircha', 'Milagai', 'Menasinkaayi'], ARRAY['Mirchi', 'Mircha', 'Green Chili', 'Red Chili']),
('Sugarcane', 'Cash Crop', 'Saccharum officinarum', 'quintal', ARRAY['Ganna', 'Oos', 'Karumbu'], ARRAY['Ganna', 'Oos']),
('Maize', 'Cereal', 'Zea mays', 'quintal', ARRAY['Makka', 'Bhutta', 'Makki', 'Cholam'], ARRAY['Makka', 'Corn', 'Bhutta']),
('Groundnut', 'Oilseed', 'Arachis hypogaea', 'quintal', ARRAY['Moongphali', 'Shengdana', 'Verkadalai'], ARRAY['Moongphali', 'Peanut', 'Shengdana']),
('Turmeric', 'Spice', 'Curcuma longa', 'quintal', ARRAY['Haldi', 'Halad', 'Manjal'], ARRAY['Haldi', 'Halad']),
('Garlic', 'Vegetable', 'Allium sativum', 'kg', ARRAY['Lahsun', 'Lasun', 'Poondu'], ARRAY['Lahsun', 'Lasun']),
('Ginger', 'Spice', 'Zingiber officinale', 'kg', ARRAY['Adrak', 'Ale', 'Inji'], ARRAY['Adrak', 'Ale']),
('Cauliflower', 'Vegetable', 'Brassica oleracea', 'kg', ARRAY['Phool Gobhi', 'Fool Kobi', 'Hookosu'], ARRAY['Phool Gobhi', 'Gobi']),
('Cabbage', 'Vegetable', 'Brassica oleracea var. capitata', 'kg', ARRAY['Patta Gobhi', 'Kobi', 'Muttaikose'], ARRAY['Patta Gobhi', 'Band Gobhi']),
('Brinjal', 'Vegetable', 'Solanum melongena', 'kg', ARRAY['Baingan', 'Vangi', 'Kathirikai', 'Badanekayi'], ARRAY['Baingan', 'Eggplant', 'Aubergine']),
('Banana', 'Fruit', 'Musa acuminata', 'kg', ARRAY['Kela', 'Keli', 'Vazhaipazham'], ARRAY['Kela', 'Keli']),
('Mango', 'Fruit', 'Mangifera indica', 'kg', ARRAY['Aam', 'Amba', 'Maampazhm', 'Maavina Hannu'], ARRAY['Aam', 'Amba']),
('Grape', 'Fruit', 'Vitis vinifera', 'kg', ARRAY['Angur', 'Draksha', 'Drakshi'], ARRAY['Angur', 'Draksha']),
('Pomegranate', 'Fruit', 'Punica granatum', 'kg', ARRAY['Anar', 'Dalimba', 'Mathulai'], ARRAY['Anar', 'Dalimba']),
('Cumin', 'Spice', 'Cuminum cyminum', 'kg', ARRAY['Jeera', 'Jire', 'Jeerakam'], ARRAY['Jeera', 'Jire']),
('Coriander', 'Spice', 'Coriandrum sativum', 'kg', ARRAY['Dhaniya', 'Kothimbir', 'Kothamalli'], ARRAY['Dhaniya', 'Dhania']),
('Mustard', 'Oilseed', 'Brassica juncea', 'quintal', ARRAY['Sarson', 'Mohri', 'Kadugu'], ARRAY['Sarson', 'Rai', 'Mohri']),
('Green Pea', 'Vegetable', 'Pisum sativum', 'kg', ARRAY['Matar', 'Vatana', 'Pattani'], ARRAY['Matar', 'Peas']),
('Lady Finger', 'Vegetable', 'Abelmoschus esculentus', 'kg', ARRAY['Bhindi', 'Bhende', 'Vendaikkai'], ARRAY['Bhindi', 'Okra']),
('Capsicum', 'Vegetable', 'Capsicum annuum (bell)', 'kg', ARRAY['Shimla Mirch', 'Dhobli Mirchi', 'Kudamilagai'], ARRAY['Shimla Mirch', 'Bell Pepper']),
('Lemon', 'Fruit', 'Citrus limon', 'kg', ARRAY['Nimbu', 'Limbu', 'Elumichai'], ARRAY['Nimbu', 'Lime']),
('Coconut', 'Plantation', 'Cocos nucifera', 'piece', ARRAY['Nariyal', 'Naral', 'Thengai', 'Tenginakayi'], ARRAY['Nariyal', 'Naral']),
('Jowar', 'Cereal', 'Sorghum bicolor', 'quintal', ARRAY['Jwari', 'Jola', 'Cholam'], ARRAY['Sorghum', 'Jwari']);
