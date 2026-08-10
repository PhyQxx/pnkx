/**
 * 角色头像和名称
 * @type {[{name: string, url: string}, {name: string, url: string}, {name: string, url: string}, {name: string, url: string}, {name: string, url: string}, null, null, null, null, null, null, null, null, null]}
 */
const header = [
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像63366-jpg.jpg',
        name: '佐助'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (13)67788-13jpg.jpg',
        name: '宁次'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (12)64114-12jpg.jpg',
        name: '卡卡西'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (11)66547-11jpg.jpg',
        name: '佐助'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (10)62953-10jpg.jpg',
        name: '大蛇丸'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (9)67679-9jpg.jpg',
        name: '鸣人'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (8)59447-8jpg.jpg',
        name: '鼬'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (7)74738-7jpg.jpg',
        name: '鸣人'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (6)71598-6jpg.jpg',
        name: '琳'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (5)70322-5jpg.jpg',
        name: '带土'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (4)64799-4jpg.jpg',
        name: '我爱罗'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (3)69964-3jpg.jpg',
        name: '小樱'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (2)75847-2jpg.jpg',
        name: '水门'
    },
    {
        url: 'https://ftp.pnkx.top:8/ftp/2023/03/13/高清超酷的动漫火影忍者图片头像 (1)62069-1jpg.jpg',
        name: '卡卡西'
    }
]
export function getPersonInfo() {
    let minimum = 0;
    let maximum = header.length - 1;
    return header[Math.floor(Math.random() * (maximum - minimum + 1)) + minimum]
}
